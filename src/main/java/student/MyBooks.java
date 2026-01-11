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
import org.bson.json.JsonWriterSettings;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet(name = "student.MyBooks", value = "/myBooks")
public class MyBooks extends HttpServlet {

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "dbLibraryMS";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedIn") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Not logged in\"}");
            return;
        }

        String studentNumber = (String) session.getAttribute("userSNumber");

        try (PrintWriter out = response.getWriter()) {
            String json;
            if (DatabaseConfig.isMongoDB()) {
                json = getMyBooksMongoDB(studentNumber);
            } else {
                json = getMyBooksSQL(studentNumber);
            }
            response.setStatus(HttpServletResponse.SC_OK);
            out.write(json);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private String getMyBooksMongoDB(String studentNumber) {
        try (MongoClient client = MongoClients.create(MONGO_URI)) {
            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<Document> col = db.getCollection("BorrowReturnHist");

            List<Document> borrowedBooks = new ArrayList<>();
            col.find(new Document("SNumber", studentNumber).append("status", "borrowed"))
                    .into(borrowedBooks);

            JsonWriterSettings settings = JsonWriterSettings.builder().build();
            return borrowedBooks.stream()
                    .map(doc -> doc.toJson(settings))
                    .collect(Collectors.joining(",", "[", "]"));
        }
    }

    private String getMyBooksSQL(String studentNumber) throws SQLException {
        String sql = "SELECT h.*, b.title, b.isbn FROM BorrowReturnHist h " +
                "JOIN Books b ON h.book_id = b.book_id " +
                "WHERE h.SNumber = ? AND h.status = 'borrowed'";

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentNumber);
            ResultSet rs = stmt.executeQuery();

            JSONArray books = new JSONArray();

            while (rs.next()) {
                JSONObject book = new JSONObject();
                book.put("isbn", rs.getString("isbn"));
                book.put("title", rs.getString("title"));
                book.put("SNumber", rs.getString("SNumber"));
                book.put("firstName", rs.getString("firstName"));
                book.put("lastName", rs.getString("lastName"));
                book.put("borrowDate", rs.getDate("borrowDate") != null ? rs.getDate("borrowDate").toString() : null);
                book.put("expectedReturnDate", rs.getDate("expectedReturnDate") != null ? rs.getDate("expectedReturnDate").toString() : null);
                book.put("actualReturnDate", rs.getDate("actualReturnDate") != null ? rs.getDate("actualReturnDate").toString() : null);
                book.put("status", rs.getString("status"));
                books.put(book);
            }

            return books.toString();
        }
    }
}
