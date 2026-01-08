package student;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import configs.DatabaseConfig;
import configs.SQLClientProvider;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.bson.Document;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@WebServlet(name = "student.BorrowBook", value = "/borrowBook")
public class BorrowBook extends HttpServlet {

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "dbLibraryMS";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        String isbn = request.getParameter("isbn");
        String title = request.getParameter("title");

        if (isbn == null || isbn.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"ISBN is required\"}");
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedIn") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Not logged in\"}");
            return;
        }

        String studentNumber = (String) session.getAttribute("userSNumber");
        String firstName = (String) session.getAttribute("userFName");
        String lastName = (String) session.getAttribute("userLName");

        try (PrintWriter out = response.getWriter()) {
            String json;
            if (DatabaseConfig.isMongoDB()) {
                json = borrowBookMongoDB(isbn, title, studentNumber, firstName, lastName);
            } else {
                json = borrowBookSQL(isbn, title, studentNumber, firstName, lastName);
            }

            if (json.contains("\"error\"")) {
                if (json.contains("not found")) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                } else if (json.contains("No copies")) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
            }
            out.write(json);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private String borrowBookMongoDB(String isbn, String title, String studentNumber, String firstName, String lastName) {
        try (MongoClient client = MongoClients.create(MONGO_URI)) {
            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<Document> booksCol = db.getCollection("Books");
            MongoCollection<Document> histCol = db.getCollection("BorrowReturnHist");

            Document book = booksCol.find(new Document("isbn", isbn)).first();
            if (book == null) {
                return "{\"error\":\"Book not found\"}";
            }

            int available = book.getInteger("available", 0);
            if (available <= 0) {
                return "{\"error\":\"No copies available\"}";
            }

            LocalDate borrowDate = LocalDate.now();
            LocalDate expectedReturnDate = borrowDate.plusDays(1);

            Document borrowRecord = new Document()
                    .append("isbn", isbn)
                    .append("title", title)
                    .append("SNumber", studentNumber)
                    .append("firstName", firstName)
                    .append("lastName", lastName)
                    .append("borrowDate", Date.from(borrowDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    .append("expectedReturnDate", Date.from(expectedReturnDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    .append("actualReturnDate", null)
                    .append("status", "borrowed");

            histCol.insertOne(borrowRecord);

            booksCol.updateOne(
                    new Document("isbn", isbn),
                    new Document("$inc", new Document("available", -1))
            );

            return "{\"success\":true,\"message\":\"Book borrowed successfully\"}";
        }
    }

    private String borrowBookSQL(String isbn, String title, String studentNumber, String firstName, String lastName) throws SQLException {
        try (Connection conn = SQLClientProvider.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Find book by ISBN
                String findBookSql = "SELECT book_id, available FROM Books WHERE isbn = ?";
                int bookId;
                int available;

                try (PreparedStatement stmt = conn.prepareStatement(findBookSql)) {
                    stmt.setString(1, isbn);
                    ResultSet rs = stmt.executeQuery();
                    if (!rs.next()) {
                        conn.rollback();
                        return "{\"error\":\"Book not found\"}";
                    }
                    bookId = rs.getInt("book_id");
                    available = rs.getInt("available");
                }

                if (available <= 0) {
                    conn.rollback();
                    return "{\"error\":\"No copies available\"}";
                }

                LocalDate borrowDate = LocalDate.now();
                LocalDate expectedReturnDate = borrowDate.plusDays(1);

                // Insert borrow record
                String insertSql = "INSERT INTO BorrowReturnHist (book_id, SNumber, firstName, lastName, " +
                        "borrowDate, expectedReturnDate, actualReturnDate, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    stmt.setInt(1, bookId);
                    stmt.setString(2, studentNumber);
                    stmt.setString(3, firstName);
                    stmt.setString(4, lastName);
                    stmt.setDate(5, java.sql.Date.valueOf(borrowDate));
                    stmt.setDate(6, java.sql.Date.valueOf(expectedReturnDate));
                    stmt.setNull(7, Types.DATE);
                    stmt.setString(8, "borrowed");
                    stmt.executeUpdate();
                }

                // Decrement available count
                String updateSql = "UPDATE Books SET available = available - 1 WHERE book_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setInt(1, bookId);
                    stmt.executeUpdate();
                }

                conn.commit();
                return "{\"success\":true,\"message\":\"Book borrowed successfully\"}";

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
