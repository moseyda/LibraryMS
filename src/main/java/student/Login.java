package student;

import com.mongodb.client.*;
import configs.DatabaseConfig;
import configs.SQLClientProvider;
import org.bson.Document;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.mindrot.jbcrypt.BCrypt;

@WebServlet(name = "student.Login", value = "/login")
public class Login extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/student/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String snumber = request.getParameter("snumber");
        String password = request.getParameter("password");

        String[] userData;
        if (DatabaseConfig.isMongoDB()) {
            userData = authenticateUserMongoDB(snumber, password);
        } else {
            userData = authenticateUserSQL(snumber, password);
        }

        if (userData != null) {
            HttpSession session = request.getSession();
            session.setAttribute("loggedIn", true);
            session.setAttribute("userFName", userData[0]);
            session.setAttribute("userLName", userData[1]);
            session.setAttribute("userSNumber", userData[2]);

            response.sendRedirect(request.getContextPath() + "/student/studentDashboard.jsp");
        } else {
            request.setAttribute("errorMessage", "Invalid Student Number or Password. Please try again.");
            request.getRequestDispatcher("/student/login.jsp").forward(request, response);
        }
    }

    private String[] authenticateUserMongoDB(String snumber, String password) {
        try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongo.getDatabase("dbLibraryMS");
            MongoCollection<Document> collection = database.getCollection("Students");

            Document query = new Document("SNumber", snumber);
            Document user = collection.find(query).first();

            if (user != null) {
                String storedHashedPassword = user.getString("Password");

                if (BCrypt.checkpw(password, storedHashedPassword)) {
                    System.out.println("User authenticated: " +
                            user.getString("FName") + " " + user.getString("LName"));

                    return new String[]{
                            user.getString("FName"),
                            user.getString("LName"),
                            user.getString("SNumber")
                    };
                }
            }

            System.out.println("Authentication failed for Student Number: " + snumber);
            return null;

        } catch (Exception e) {
            System.err.println("Error during authentication: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String[] authenticateUserSQL(String snumber, String password) {
        String sql = "SELECT FName, LName, SNumber, Password FROM Students WHERE SNumber = ?";

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, snumber);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHashedPassword = rs.getString("Password");

                if (BCrypt.checkpw(password, storedHashedPassword)) {
                    String fname = rs.getString("FName");
                    String lname = rs.getString("LName");
                    String snum = rs.getString("SNumber");

                    System.out.println("User authenticated: " + fname + " " + lname);
                    return new String[]{fname, lname, snum};
                }
            }

            System.out.println("Authentication failed for Student Number: " + snumber);
            return null;

        } catch (SQLException e) {
            System.err.println("Error during authentication: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
