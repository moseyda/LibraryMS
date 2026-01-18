package admin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import configs.DatabaseConfig;
import configs.SQLClientProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "admin.GetBooks", value = "/admin/getBooks")
public class GetBooks extends HttpServlet {

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "dbLibraryMS";
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        JsonObject result = new JsonObject();
        JsonArray booksArray = new JsonArray();

        try {
            if (DatabaseConfig.isMongoDB()) {
                // MongoDB mode — return _id as object with $oid to match SQL branch/front-end expectations
                try (MongoClient mongo = MongoClients.create(MONGO_URI)) {
                    MongoDatabase database = mongo.getDatabase(DB_NAME);
                    MongoCollection<Document> collection = database.getCollection("Books");

                    for (Document doc : collection.find()) {
                        JsonObject book = new JsonObject();

                        JsonObject idObj = new JsonObject();
                        idObj.addProperty("$oid", doc.getObjectId("_id").toString());
                        book.add("_id", idObj);

                        book.addProperty("title", doc.getString("title"));
                        book.addProperty("author", doc.getString("author"));
                        book.addProperty("isbn", doc.getString("isbn"));
                        book.addProperty("category", doc.getString("category"));
                        book.addProperty("publisher", doc.getString("publisher"));
                        book.addProperty("publicationYear", doc.getInteger("publicationYear", 0));
                        book.addProperty("quantity", doc.getInteger("quantity", 0));
                        book.addProperty("available", doc.getInteger("available", 0));
                        book.addProperty("description", doc.getString("description"));
                        booksArray.add(book);
                    }
                }
            } else {
                // SQL mode (unchanged)
                try (Connection conn = SQLClientProvider.getConnection()) {

                    String sql =
                            "SELECT book_id, title, author, isbn, category, publisher, " +
                                    "publicationYear, quantity, available, description " +
                                    "FROM Books";

                    try (PreparedStatement stmt = conn.prepareStatement(sql);
                         ResultSet rs = stmt.executeQuery()) {

                        while (rs.next()) {
                            JsonObject book = new JsonObject();

                            JsonObject idObj = new JsonObject();
                            idObj.addProperty("$oid", String.valueOf(rs.getInt("book_id")));
                            book.add("_id", idObj);

                            // same field names used in Mongo branch
                            book.addProperty("title", rs.getString("title"));
                            book.addProperty("author", rs.getString("author"));
                            book.addProperty("isbn", rs.getString("isbn"));
                            book.addProperty("category", rs.getString("category"));
                            book.addProperty("publisher", rs.getString("publisher"));
                            book.addProperty("publicationYear", rs.getInt("publicationYear"));
                            book.addProperty("quantity", rs.getInt("quantity"));
                            book.addProperty("available", rs.getInt("available"));
                            book.addProperty("description", rs.getString("description"));

                            booksArray.add(book);
                        }
                    }
                }
            }

            result.add("books", booksArray);
            result.addProperty("success", true);
            resp.getWriter().write(gson.toJson(result));

        } catch (Exception e) {
            e.printStackTrace();
            result.addProperty("success", false);
            result.addProperty("error", e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(gson.toJson(result));
        }
    }
}
