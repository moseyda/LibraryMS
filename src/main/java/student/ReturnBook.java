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
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@WebServlet(name = "student.ReturnBook", value = "/returnBook")
public class ReturnBook extends HttpServlet {

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "dbLibraryMS";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        String borrowId = request.getParameter("borrowId");

        try (PrintWriter out = response.getWriter()) {
            boolean success;
            String errorMsg = null;

            if (DatabaseConfig.isMongoDB()) {
                Object[] result = returnBookMongoDB(borrowId);
                success = (boolean) result[0];
                errorMsg = (String) result[1];
            } else {
                Object[] result = returnBookSQL(borrowId);
                success = (boolean) result[0];
                errorMsg = (String) result[1];
            }

            if (errorMsg != null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\":\"" + errorMsg + "\"}");
            } else if (success) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.write("{\"success\":true,\"message\":\"Book returned successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("{\"error\":\"Failed to return book\"}");
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private Object[] returnBookMongoDB(String borrowId) {
        try (MongoClient client = MongoClients.create(MONGO_URI)) {
            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<Document> histCol = db.getCollection("BorrowReturnHist");
            MongoCollection<Document> booksCol = db.getCollection("Books");

            Document borrowRecord = histCol.find(new Document("_id", new ObjectId(borrowId))).first();
            if (borrowRecord == null) {
                return new Object[]{false, "Borrow record not found"};
            }

            String isbn = borrowRecord.getString("isbn");

            Date actualReturnDate = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());

            histCol.updateOne(
                    new Document("_id", new ObjectId(borrowId)),
                    new Document("$set", new Document()
                            .append("status", "returned")
                            .append("actualReturnDate", actualReturnDate))
            );

            booksCol.updateOne(
                    new Document("isbn", isbn),
                    new Document("$inc", new Document("available", 1))
            );

            return new Object[]{true, null};
        } catch (Exception e) {
            return new Object[]{false, e.getMessage()};
        }
    }

    private Object[] returnBookSQL(String borrowId) throws SQLException {
        try (Connection conn = SQLClientProvider.getConnection()) {
            // Get the borrow record to find book_id
            String selectSql = "SELECT book_id FROM BorrowReturnHist WHERE id = ?";
            int bookId;

            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setInt(1, Integer.parseInt(borrowId));
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    return new Object[]{false, "Borrow record not found"};
                }
                bookId = rs.getInt("book_id");
            }

            Timestamp actualReturnDate = Timestamp.valueOf(LocalDateTime.now());

            // Update borrow record
            String updateHistSql = "UPDATE BorrowReturnHist SET status = 'returned', actualReturnDate = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateHistSql)) {
                stmt.setTimestamp(1, actualReturnDate);
                stmt.setInt(2, Integer.parseInt(borrowId));
                stmt.executeUpdate();
            }

            // Increment available count
            String updateBookSql = "UPDATE Books SET available = available + 1 WHERE book_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateBookSql)) {
                stmt.setInt(1, bookId);
                stmt.executeUpdate();
            }

            return new Object[]{true, null};
        }
    }
}
