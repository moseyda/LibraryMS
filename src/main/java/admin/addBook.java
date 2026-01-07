package admin;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
import configs.DatabaseConfig;
import configs.SQLClientProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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

            int quantity = Integer.parseInt(quantityStr);
            int available = Integer.parseInt(availableStr);
            Integer publicationYear = (publicationYearStr != null && !publicationYearStr.trim().isEmpty())
                    ? Integer.parseInt(publicationYearStr) : null;

            boolean success;
            if (DatabaseConfig.isMongoDB()) {
                success = addBookMongoDB(title, author, isbn, category, publisher, publicationYear, quantity, available, description);
            } else {
                success = addBookSQL(title, author, isbn, category, publisher, publicationYear, quantity, available, description);
            }

            if (success) {
                System.out.println("SUCCESS: Book added");
                out.write("{\"success\": true, \"message\": \"Book added successfully\"}");
            } else {
                System.out.println("FAILED: Insert operation failed");
                out.write("{\"success\": false, \"error\": \"Insert operation failed\"}");
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

    // ################################################################## //
    // ###############      MongoDB implementation        ############### //
    // ################################################################## //

    private boolean addBookMongoDB(String title, String author, String isbn, String category,
                                   String publisher, Integer publicationYear, int quantity,
                                   int available, String description) {
        try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongo.getDatabase("dbLibraryMS");
            MongoCollection<Document> collection = database.getCollection("Books");

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
            return result.wasAcknowledged();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    // ################################################################## //
    // ###############      SQL implementation            ############### //
    // ################################################################## //


    private boolean addBookSQL(String title, String author, String isbn, String category,
                               String publisher, Integer publicationYear, int quantity,
                               int available, String description) {
        String sql = "INSERT INTO Books (title, author, isbn, category, publisher, publicationYear, quantity, available, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, title.trim());
            stmt.setString(2, author.trim());
            stmt.setString(3, isbn.trim());
            stmt.setString(4, category.trim());
            stmt.setString(5, publisher != null ? publisher.trim() : "");
            if (publicationYear != null) {
                stmt.setInt(6, publicationYear);
            } else {
                stmt.setNull(6, java.sql.Types.INTEGER);
            }
            stmt.setInt(7, quantity);
            stmt.setInt(8, available);
            stmt.setString(9, description != null ? description.trim() : "");

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
