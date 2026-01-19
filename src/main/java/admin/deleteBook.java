package admin;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import configs.DatabaseConfig;
import configs.SQLClientProvider;
import jakarta.servlet.ServletException;
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
import java.sql.SQLException;

@WebServlet(name = "deleteBook", value = "/admin/deleteBook")
public class deleteBook extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (PrintWriter out = response.getWriter()) {

            String bookId = request.getParameter("bookId");
            System.out.println("Deleting book ID: " + bookId);

            /* ---- Basic validation (unchanged user message) ---- */
            if (bookId == null || bookId.trim().isEmpty()) {
                out.write("{\"success\": false, \"error\": \"Book ID is required\"}");
                return;
            }

            boolean success;
            if (DatabaseConfig.isMongoDB()) {
                success = deleteBookMongoDB(bookId);
            } else {
                success = deleteBookSQL(bookId);
            }

            if (success) {
                System.out.println("SUCCESS: Book deleted");
                out.write("{\"success\": true, \"message\": \"Book deleted successfully\"}");
            } else {
                System.out.println("FAILED: Book not found");
                out.write("{\"success\": false, \"error\": \"Book not found\"}");
            }

        } catch (IllegalArgumentException e) {
            // Covers invalid ObjectId OR invalid numeric ID
            System.out.println("Invalid book ID: " + e.getMessage());
            response.getWriter()
                    .write("{\"success\": false, \"error\": \"Invalid book ID\"}");

        } catch (Exception e) {
            // True unexpected failures only
            e.printStackTrace();
            response.getWriter()
                    .write("{\"success\": false, \"error\": \"Internal server error\"}");
        }
    }

    /* ===================== MongoDB ===================== */
    private boolean deleteBookMongoDB(String bookId) {

        // Explicit ObjectId validation (cleaner failure mode)
        if (!ObjectId.isValid(bookId)) {
            throw new IllegalArgumentException("Invalid MongoDB ObjectId");
        }

        try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {

            MongoDatabase database = mongo.getDatabase("dbLibraryMS");
            MongoCollection<Document> collection = database.getCollection("Books");

            DeleteResult result =
                    collection.deleteOne(new Document("_id", new ObjectId(bookId)));

            return result.wasAcknowledged() && result.getDeletedCount() > 0;
        }
    }

    /* ===================== SQL ===================== */
    private boolean deleteBookSQL(String bookId) {

        String sql = "DELETE FROM Books WHERE book_id = ?";

        int parsedId;
        try {
            parsedId = Integer.parseInt(bookId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid SQL book ID");
        }

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, parsedId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
