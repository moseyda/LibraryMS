package model;

import com.mongodb.client.*;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "updateBook", value = "/admin/updateBook")
public class updateBook extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Check admin authentication
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("adminLoggedIn") == null) {
            response.sendRedirect(request.getContextPath() + "/admin/login");
            return;
        }

        // Get form parameters
        String bookId = request.getParameter("bookId");
        String title = request.getParameter("title");
        String author = request.getParameter("author");
        String isbn = request.getParameter("isbn");
        String category = request.getParameter("category");
        String publisher = request.getParameter("publisher");
        String publicationYear = request.getParameter("publicationYear");
        int quantity = Integer.parseInt(request.getParameter("quantity"));
        int available = Integer.parseInt(request.getParameter("available"));
        String description = request.getParameter("description");

        // Create update document
        Document updates = new Document()
                .append("title", title)
                .append("author", author)
                .append("isbn", isbn)
                .append("category", category)
                .append("publisher", publisher)
                .append("publicationYear", publicationYear != null && !publicationYear.isEmpty() ? Integer.parseInt(publicationYear) : null)
                .append("quantity", quantity)
                .append("available", available)
                .append("description", description)
                .append("updatedAt", new java.util.Date());

        // Update in database
        boolean success = updateBook(bookId, updates);

        if (success) {
            response.sendRedirect(request.getContextPath() + "/staff/adminDashboard.jsp?success=updated");
        } else {
            response.sendRedirect(request.getContextPath() + "/staff/adminDashboard.jsp?error=update_failed");
        }
    }

    private boolean updateBook(String bookId, Document updates) {
        try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongo.getDatabase("LibraryMS");
            MongoCollection<Document> collection = database.getCollection("Books");

            Document query = new Document("_id", new ObjectId(bookId));
            Document update = new Document("$set", updates);

            collection.updateOne(query, update);
            System.out.println("Book updated: " + bookId);
            return true;
        } catch (Exception e) {
            System.err.println("Error updating book: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}