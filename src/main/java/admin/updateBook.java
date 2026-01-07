package admin;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
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

@WebServlet(name = "updateBook", value = "/admin/updateBook")
public class updateBook extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String bookId = request.getParameter("bookId");
            String title = request.getParameter("title");
            String author = request.getParameter("author");
            String isbn = request.getParameter("isbn");
            String category = request.getParameter("category");
            String publisher = request.getParameter("publisher");
            String publicationYearStr = request.getParameter("publicationYear");
            String quantityStr = request.getParameter("quantity");
            String availableStr = request.getParameter("available");
            String description = request.getParameter("description");

            System.out.println("Updating book ID: " + bookId);

            if (bookId == null || bookId.trim().isEmpty() ||
                    title == null || title.trim().isEmpty() ||
                    author == null || author.trim().isEmpty() ||
                    isbn == null || isbn.trim().isEmpty() ||
                    category == null || category.trim().isEmpty() ||
                    quantityStr == null || quantityStr.trim().isEmpty() ||
                    availableStr == null || availableStr.trim().isEmpty()) {

                System.out.println("Validation failed - missing fields");
                out.write("{\"success\": false, \"error\": \"Missing required fields\"}");
                return;
            }

            int quantity = Integer.parseInt(quantityStr);
            int available = Integer.parseInt(availableStr);
            Integer publicationYear = (publicationYearStr != null && !publicationYearStr.trim().isEmpty())
                    ? Integer.parseInt(publicationYearStr) : null;

            int result;
            if (DatabaseConfig.isMongoDB()) {
                result = updateBookMongoDB(bookId, title, author, isbn, category, publisher, publicationYear, quantity, available, description);
            } else {
                result = updateBookSQL(bookId, title, author, isbn, category, publisher, publicationYear, quantity, available, description);
            }

            if (result > 0) {
                System.out.println("SUCCESS: Book updated");
                out.write("{\"success\": true, \"message\": \"Book updated successfully\"}");
            } else if (result == 0) {
                System.out.println("Book found but no changes made");
                out.write("{\"success\": true, \"message\": \"No changes made\"}");
            } else {
                System.out.println("FAILED: Book not found");
                out.write("{\"success\": false, \"error\": \"Book not found\"}");
            }

        } catch (NumberFormatException e) {
            System.out.println("Number format error: " + e.getMessage());
            out.write("{\"success\": false, \"error\": \"Invalid number format\"}");
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

    // MongoDB implementation - returns: 1=modified, 0=matched but no change, -1=not found
    private int updateBookMongoDB(String bookId, String title, String author, String isbn,
                                  String category, String publisher, Integer publicationYear,
                                  int quantity, int available, String description) {
        try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongo.getDatabase("dbLibraryMS");
            MongoCollection<Document> collection = database.getCollection("Books");

            Document updateDoc = new Document("$set", new Document("title", title.trim())
                    .append("author", author.trim())
                    .append("isbn", isbn.trim())
                    .append("category", category.trim())
                    .append("publisher", publisher != null ? publisher.trim() : "")
                    .append("publicationYear", publicationYear)
                    .append("quantity", quantity)
                    .append("available", available)
                    .append("description", description != null ? description.trim() : ""));

            UpdateResult result = collection.updateOne(
                    new Document("_id", new ObjectId(bookId)),
                    updateDoc
            );

            if (result.getModifiedCount() > 0) return 1;
            if (result.getMatchedCount() > 0) return 0;
            return -1;
        }
    }

    // SQL implementation - returns: 1=modified, 0=matched but no change, -1=not found
    private int updateBookSQL(String bookId, String title, String author, String isbn,
                              String category, String publisher, Integer publicationYear,
                              int quantity, int available, String description) {
        String sql = "UPDATE Books SET title = ?, author = ?, isbn = ?, category = ?, " +
                "publisher = ?, publicationYear = ?, quantity = ?, available = ?, description = ? " +
                "WHERE book_id = ?";

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, title.trim());
            stmt.setString(2, author.trim());
            stmt.setString(3, isbn.trim());
            stmt.setString(4, category.trim());
            stmt.setString(5, publisher != null ? publisher.trim() : "");
            if (publicationYear != null) {
                stmt.setInt(6, publicationYear);
            } else {
                stmt.setNull(6, java.sql.Types.INTEGER);
            }
            stmt.setInt(7, quantity);
            stmt.setInt(8, available);
            stmt.setString(9, description != null ? description.trim() : "");
            stmt.setInt(10, Integer.parseInt(bookId));

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0 ? 1 : -1;

        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
