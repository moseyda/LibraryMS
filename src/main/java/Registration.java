import com.mongodb.client.*;
import org.bson.Document;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;




@WebServlet (name = "Registration", value = "/registration")

public class Registration extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html><head><title>Hello Servlet</title></head>");
            out.println("<body>");
            out.println("<h1>Hello from Jakarta EE Servlet!</h1>");
            out.println("<p>Request method: GET</p>");
            out.println("</body></html>");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        out.println("Processing user registration ........");

        // Capture all form fields including Student Number
        Document customer = new Document()
                .append("FName", request.getParameter("fname"))
                .append("LName", request.getParameter("lname"))
                .append("SNumber", request.getParameter("snumber"));

        createCustomer(customer);

        out.println("<br>User registered successfully!");

    }

    public void createCustomer(Document customer) {
        // Use try-with-resources to ensure connection is closed
        try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongo.getDatabase("dbStudents");
            MongoCollection<Document> collection = database.getCollection("Collection_Registration");
            collection.insertOne(customer);
            System.out.println("Document inserted: " + customer.toJson());
        } catch (Exception e) {
            System.err.println("Error inserting document: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
