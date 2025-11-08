package admin;

import com.mongodb.client.*;
import org.bson.Document;

import java.io.IOException;
import java.util.Date;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "AdminLogin", value = "/admin/login")
public class adminLogin extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/admin/adminLogin.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Authenticate admin
        Document admin = authenticateAdmin(username, password);

        if (admin != null) {
            // Check if admin is active
            Boolean isActive = admin.getBoolean("isActive");
            if (isActive != null && isActive) {
                // Update last login
                updateLastLogin(username);

                // Create admin session
                HttpSession session = request.getSession();
                session.setAttribute("adminLoggedIn", true);
                session.setAttribute("adminUsername", admin.getString("username"));
                session.setAttribute("adminRole", admin.getString("role"));

                // Redirect to admin dashboard
                response.sendRedirect(request.getContextPath() + "/staff/adminDashboard.jsp");
            } else {
                request.setAttribute("errorMessage", "Your admin account is inactive. Please contact the system administrator.");
                request.getRequestDispatcher("/admin/adminLogin.jsp").forward(request, response);
            }
        } else {
            request.setAttribute("errorMessage", "Invalid username or password.");
            request.getRequestDispatcher("/admin/adminLogin.jsp").forward(request, response);
        }
    }

    private Document authenticateAdmin(String username, String password) {
        try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongo.getDatabase("dbLibraryMS");
            MongoCollection<Document> collection = database.getCollection("Admin");

            // Debug: Print what we're searching for
            System.out.println("=== Authentication Debug ===");
            System.out.println("Attempting to authenticate username: " + username);
            System.out.println("Password length: " + (password != null ? password.length() : "null"));

            // First, let's see all documents in the Admin collection
            System.out.println("All documents in Admin collection:");
            for (Document doc : collection.find()) {
                System.out.println("Found admin document: " + doc.toJson());
            }

            // Now search for the specific user
            Document userQuery = new Document("username", username);
            Document foundUser = collection.find(userQuery).first();

            if (foundUser != null) {
                System.out.println("User found in database: " + foundUser.toJson());
                String storedPassword = foundUser.getString("password");
                System.out.println("Stored password: '" + storedPassword + "'");
                System.out.println("Provided password: '" + password + "'");
                System.out.println("Passwords match: " + (storedPassword != null && storedPassword.equals(password)));

                // Check if passwords match
                if (storedPassword != null && storedPassword.equals(password)) {
                    System.out.println("Admin authenticated: " + foundUser.getString("username"));
                    return foundUser;
                } else {
                    System.out.println("Password mismatch for username: " + username);
                    return null;
                }
            } else {
                System.out.println("User not found in database: " + username);
            }

            return null;
        } catch (Exception e) {
            System.err.println("Error during admin authentication: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void updateLastLogin(String username) {
        try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongo.getDatabase("dbLibraryMS");
            MongoCollection<Document> collection = database.getCollection("Admin");

            Document query = new Document("username", username);
            Document update = new Document("$set", new Document("lastLogin", new Date()));

            collection.updateOne(query, update);
            System.out.println("Updated last login for admin: " + username);
        } catch (Exception e) {
            System.err.println("Error updating last login: " + e.getMessage());
            e.printStackTrace();
        }
    }
}