package admin;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.io.PrintWriter;

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

            try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
                MongoDatabase database = mongo.getDatabase("dbLibraryMS");
                MongoCollection<Document> collection = database.getCollection("Books");

                int quantity = Integer.parseInt(quantityStr);
                int available = Integer.parseInt(availableStr);
                Integer publicationYear = (publicationYearStr != null && !publicationYearStr.trim().isEmpty())
                        ? Integer.parseInt(publicationYearStr) : null;

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

                if (result.getModifiedCount() > 0) {
                    System.out.println("SUCCESS: Book updated");
                    out.write("{\"success\": true, \"message\": \"Book updated successfully\"}");
                } else if (result.getMatchedCount() > 0) {
                    System.out.println("Book found but no changes made");
                    out.write("{\"success\": true, \"message\": \"No changes made\"}");
                } else {
                    System.out.println("FAILED: Book not found");
                    out.write("{\"success\": false, \"error\": \"Book not found\"}");
                }
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
}
