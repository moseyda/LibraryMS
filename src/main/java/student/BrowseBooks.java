package student;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import configs.DatabaseConfig;
import configs.SQLClientProvider;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet(name = "student.BrowseBooks", value = "/browseBooks")
public class BrowseBooks extends HttpServlet {

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "dbLibraryMS";
    private static final String COLLECTION_NAME = "Books";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        try (PrintWriter out = response.getWriter()) {
            String json;
            if (DatabaseConfig.isMongoDB()) {
                json = browseBooksMongoDB();
            } else {
                json = browseBooksSQL();
            }
            response.setStatus(HttpServletResponse.SC_OK);
            out.write(json);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = e.getMessage() == null ? "Server error" : e.getMessage().replace("\"", "'");
            response.getWriter().write("{\"error\":\"" + msg + "\"}");
        }
    }

    private String browseBooksMongoDB() {
        try (MongoClient client = MongoClients.create(MONGO_URI)) {
            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<Document> col = db.getCollection(COLLECTION_NAME);

            List<Document> raw = new ArrayList<>();
            col.find().into(raw);

            List<Document> mapped = new ArrayList<>();
            for (Document d : raw) {
                String title = str(d, "title");
                String author = str(d, "author");
                String isbn = str(d, "isbn");
                String category = str(d, "category");
                String publisher = str(d, "publisher");
                Object publicationYear = firstNonNull(d.get("publicationYear"), d.get("year"));
                Object quantity = d.get("quantity");
                Object available = d.get("available");
                String description = str(d, "description");

                int availInt = available instanceof Number ? ((Number) available).intValue() : -1;
                String status = availInt > 0 ? "Available" : "Unavailable";

                Document outDoc = new Document()
                        .append("title", title)
                        .append("author", author)
                        .append("isbn", isbn)
                        .append("category", category)
                        .append("publisher", publisher)
                        .append("publicationYear", publicationYear)
                        .append("quantity", quantity)
                        .append("available", available)
                        .append("description", description)
                        .append("status", status);
                mapped.add(outDoc);
            }

            JsonWriterSettings settings = JsonWriterSettings.builder().build();
            return mapped.stream()
                    .map(doc -> doc.toJson(settings))
                    .collect(Collectors.joining(",", "[", "]"));
        }
    }

    private String browseBooksSQL() throws SQLException {
        String sql = "SELECT book_id, title, author, isbn, category, publisher, " +
                "publicationYear, quantity, available, description FROM Books";

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            JSONArray books = new JSONArray();

            while (rs.next()) {
                JSONObject book = new JSONObject();
                book.put("book_id", rs.getInt("book_id"));
                book.put("title", rs.getString("title"));
                book.put("author", rs.getString("author"));
                book.put("isbn", rs.getString("isbn"));
                book.put("category", rs.getString("category"));
                book.put("publisher", rs.getString("publisher"));
                book.put("publicationYear", rs.getObject("publicationYear"));
                book.put("quantity", rs.getInt("quantity"));

                int available = rs.getInt("available");
                book.put("available", available);
                book.put("description", rs.getString("description"));
                book.put("status", available > 0 ? "Available" : "Unavailable");

                books.put(book);
            }

            return books.toString();
        }
    }

    private String str(Document d, String key) {
        Object v = d.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private Object firstNonNull(Object... vals) {
        for (Object v : vals) if (v != null) return v;
        return null;
    }
}
