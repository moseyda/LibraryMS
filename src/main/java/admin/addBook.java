package admin;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

@WebServlet(name = "addBook", value = "/admin/addBook")
public class addBook extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            // Debug: Print all parameters
            System.out.println("=== Received Parameters ===");
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                System.out.println(paramName + " = " + request.getParameter(paramName));
            }

            String title = request.getParameter("title");
            String author = request.getParameter("author");
            String isbn = request.getParameter("isbn");
            String category = request.getParameter("category");
            String publisher = request.getParameter("publisher");
            String publicationYearStr = request.getParameter("publicationYear");
            String quantityStr = request.getParameter("quantity");
            String availableStr = request.getParameter("available");
            String description = request.getParameter("description");

            System.out.println("Title: " + title);
            System.out.println("Author: " + author);
            System.out.println("ISBN: " + isbn);

            if (title == null || title.trim().isEmpty() ||
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

                Document book = new Document("title", title.trim())
                        .append("author", author.trim())
                        .append("isbn", isbn.trim())
                        .append("category", category.trim())
                        .append("publisher", publisher != null ? publisher.trim() : "")
                        .append("publicationYear", publicationYear)
                        .append("quantity", quantity)
                        .append("available", available)
                        .append("description", description != null ? description.trim() : "");

                InsertOneResult result = collection.insertOne(book);

                if (result.wasAcknowledged()) {
                    System.out.println("SUCCESS: Book added with ID: " + result.getInsertedId());
                    out.write("{\"success\": true, \"message\": \"Book added successfully\"}");
                } else {
                    System.out.println("FAILED: Insert not acknowledged");
                    out.write("{\"success\": false, \"error\": \"Insert operation failed\"}");
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Number format error: " + e.getMessage());
            out.write("{\"success\": false, \"error\": \"Invalid number format\"}");
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
            out.write("{\"success\": false, \"error\": \"" + e.getMessage().replace("\"", "'") + "\"}");
        } finally {
            out.flush();
        }
    }
}
