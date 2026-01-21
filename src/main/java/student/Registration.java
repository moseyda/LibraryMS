package student;

import com.mongodb.client.*;
import configs.DatabaseConfig;
import configs.SQLClientProvider;
import org.bson.Document;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

@WebServlet(name = "Registration", value = "/registration")
public class Registration extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String fname = request.getParameter("fname");
        String lname = request.getParameter("lname");
        String snumber = request.getParameter("snumber");
        String password = request.getParameter("password");

        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            response.sendRedirect(request.getContextPath() +
                    "/student/registration.jsp?error=weak_password");
            return;
        }

        // HASH PASSWORD
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        boolean ok;
        if (DatabaseConfig.isMongoDB()) {
            ok = createCustomerMongoDB(fname, lname, snumber, hashedPassword);
        } else {
            ok = createCustomerSQL(fname, lname, snumber, hashedPassword);
        }

        if (ok) {
            response.sendRedirect(request.getContextPath() + "/student/login.jsp?registered=1");
        } else {
            response.sendRedirect(request.getContextPath() + "/student/registration.jsp?error=1");
        }
    }

    private boolean createCustomerMongoDB(String fname, String lname, String snumber, String password) {
        try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongo.getDatabase("dbLibraryMS");
            MongoCollection<Document> collection = database.getCollection("Students");

            Document customer = new Document()
                    .append("FName", fname)
                    .append("LName", lname)
                    .append("SNumber", snumber)
                    .append("Password", password);

            collection.insertOne(customer);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean createCustomerSQL(String fname, String lname, String snumber, String password) {
        String sql = "INSERT INTO Students (FName, LName, SNumber, Password) VALUES (?, ?, ?, ?)";

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fname);
            stmt.setString(2, lname);
            stmt.setString(3, snumber);
            stmt.setString(4, password);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
