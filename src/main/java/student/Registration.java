package student;

import com.mongodb.client.*;
import org.bson.Document;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet(name = "Registration", value = "/registration")
public class Registration extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Document customer = new Document()
                .append("FName", request.getParameter("fname"))
                .append("LName", request.getParameter("lname"))
                .append("SNumber", request.getParameter("snumber"))
                .append("Password", request.getParameter("password"));

        boolean ok = createCustomer(customer);

        if (ok) {
            // Redirect to login with flag
            response.sendRedirect(request.getContextPath() + "/student/login.jsp?registered=1");
        } else {
            response.sendRedirect(request.getContextPath() + "/student/registration.jsp?error=1");
        }
    }

    private boolean createCustomer(Document customer) {
        try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongo.getDatabase("dbLibraryMS");
            MongoCollection<Document> collection = database.getCollection("Students");
            collection.insertOne(customer);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
