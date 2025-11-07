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

@WebServlet(name = "deleteBook", value = "/admin/deleteBook")
public class deleteBook extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Check admin authentication
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("adminLoggedIn") == null) {
            response.sendRedirect(request.getContextPath() + "/admin/login");
            return;
        }

        String bookId = request.getParameter("bookId");

        // Delete from database
        boolean success = deleteBook(bookId);

        if (success) {
            response.sendRedirect(request.getContextPath() + "/staff/adminDashboard.jsp?success=deleted");
        } else {
            response.sendRedirect(request.getContextPath() + "/staff/adminDashboard.jsp?error=delete_failed");
        }
    }

    private boolean deleteBook(String bookId) {
        try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongo.getDatabase("LibraryMS");
            MongoCollection<Document> collection = database.getCollection("Books");

            Document query = new Document("_id", new ObjectId(bookId));
            collection.deleteOne(query);

            System.out.println("Book deleted: " + bookId);
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting book: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}