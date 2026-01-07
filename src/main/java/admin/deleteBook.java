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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String bookId = request.getParameter("bookId");

            System.out.println("Deleting book ID: " + bookId);

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
            System.out.println("Invalid book ID: " + e.getMessage());
            out.write("{\"success\": false, \"error\": \"Invalid book ID\"}");
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
            out.write("{\"success\": false, \"error\": \"" + e.getMessage().replace("\"", "'") + "\"}");
        } finally {
            out.flush();
        }
    }

    // MongoDB implementation
    private boolean deleteBookMongoDB(String bookId) {
        try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongo.getDatabase("dbLibraryMS");
            MongoCollection<Document> collection = database.getCollection("Books");

            DeleteResult result = collection.deleteOne(new Document("_id", new ObjectId(bookId)));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // SQL implementation
    private boolean deleteBookSQL(String bookId) {
        String sql = "DELETE FROM Books WHERE book_id = ?";

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, Integer.parseInt(bookId));
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }
}
