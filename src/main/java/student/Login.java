package student;

import com.mongodb.client.*;
import org.bson.Document;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "student.Login", value = "/login")
public class Login extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Forward to login page
        request.getRequestDispatcher("/student/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String snumber = request.getParameter("snumber");
        String password = request.getParameter("password");

        // Authenticate user
        Document user = authenticateUser(snumber, password);

        if (user != null) {
            // Login successful - create session
            HttpSession session = request.getSession();
            session.setAttribute("loggedIn", true);
            session.setAttribute("userFName", user.getString("FName"));
            session.setAttribute("userLName", user.getString("LName"));
            session.setAttribute("userSNumber", user.getString("SNumber"));

            // Redirect to success page
            response.sendRedirect(request.getContextPath() + "/student/studentDashboard.jsp");
        } else {
            // Login failed - send back to login with error
            request.setAttribute("errorMessage", "Invalid Student Number or Password. Please try again.");
            request.getRequestDispatcher("/student/login.jsp").forward(request, response);
        }
    }

    private Document authenticateUser(String snumber, String password) {
        try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongo.getDatabase("dbLibraryMS");
            MongoCollection<Document> collection = database.getCollection("Students");

            // Find user with matching student number and password
            Document query = new Document()
                    .append("SNumber", snumber)
                    .append("Password", password);

            Document user = collection.find(query).first();

            if (user != null) {
                System.out.println("User authenticated: " + user.getString("FName") + " " + user.getString("LName"));
            } else {
                System.out.println("Authentication failed for Student Number: " + snumber);
            }

            return user;
        } catch (Exception e) {
            System.err.println("Error during authentication: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

