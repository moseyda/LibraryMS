package admin;

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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet(name = "admin.LoansActivity", value = "/admin/loansActivity")
public class LoansActivity extends HttpServlet {

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "dbLibraryMS";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        String statusFilter = request.getParameter("status");

        try (PrintWriter out = response.getWriter()) {
            String json;
            if (DatabaseConfig.isMongoDB()) {
                json = getLoansActivityMongoDB(statusFilter);
            } else {
                json = getLoansActivitySQL(statusFilter);
            }

            response.setStatus(HttpServletResponse.SC_OK);
            out.write(json);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = e.getMessage() == null ? "Server error" : e.getMessage().replace("\"", "'");
            try (PrintWriter out = response.getWriter()) {
                out.write("{\"error\":\"" + msg + "\"}");
            } catch (Exception ignored) {}
        }
    }

    // MongoDB implementation
    private String getLoansActivityMongoDB(String statusFilter) {
        try (MongoClient client = MongoClients.create(MONGO_URI)) {
            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<Document> col = db.getCollection("BorrowReturnHist");

            List<Document> records = new ArrayList<>();
            col.find(new Document())
                    .sort(new Document("borrowDate", -1))
                    .into(records);

            Date now = new Date();
            List<Document> enriched = records.stream()
                    .map(doc -> {
                        Document copy = new Document(doc);
                        String computed = computeStatusMongo(copy, now);
                        copy.put("status", computed);
                        return copy;
                    })
                    .filter(d -> statusFilter == null || statusFilter.isBlank()
                            || statusFilter.equalsIgnoreCase(d.getString("status")))
                    .collect(Collectors.toList());

            JsonWriterSettings settings = JsonWriterSettings.builder().build();
            return enriched.stream()
                    .map(d -> d.toJson(settings))
                    .collect(Collectors.joining(",", "[", "]"));
        }
    }

    // SQL implementation
    private String getLoansActivitySQL(String statusFilter) throws Exception {
        String sql = "SELECT * FROM BorrowReturnHist ORDER BY borrowDate DESC";

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            JSONArray jsonArray = new JSONArray();
            Date now = new Date();

            while (rs.next()) {
                java.sql.Timestamp expectedReturn = rs.getTimestamp("expectedReturnDate");
                java.sql.Timestamp actualReturn = rs.getTimestamp("actualReturnDate");
                String dbStatus = rs.getString("status");

                String computedStatus = computeStatusSQL(expectedReturn, actualReturn, dbStatus, now);

                if (statusFilter != null && !statusFilter.isBlank()
                        && !statusFilter.equalsIgnoreCase(computedStatus)) {
                    continue;
                }

                JSONObject obj = new JSONObject();
                obj.put("record_id", rs.getInt("record_id"));
                obj.put("SNumber", rs.getString("SNumber"));
                obj.put("isbn", rs.getString("isbn"));
                obj.put("title", rs.getString("title"));
                obj.put("firstName", rs.getString("firstName"));
                obj.put("lastName", rs.getString("lastName"));
                obj.put("book_id", rs.getInt("book_id"));
                obj.put("borrowDate", rs.getTimestamp("borrowDate") != null ? rs.getTimestamp("borrowDate").getTime() : JSONObject.NULL);
                obj.put("expectedReturnDate", expectedReturn != null ? expectedReturn.getTime() : JSONObject.NULL);
                obj.put("actualReturnDate", actualReturn != null ? actualReturn.getTime() : JSONObject.NULL);
                obj.put("status", computedStatus);

                jsonArray.put(obj);
            }

            return jsonArray.toString();
        }
    }

    private String computeStatusMongo(Document doc, Date now) {
        Date expected = doc.getDate("expectedReturnDate");
        Date actual = doc.getDate("actualReturnDate");
        String dbStatus = doc.getString("status");

        if (actual != null) return "returned";
        if (expected != null && expected.before(now)) return "overdue";
        if (dbStatus != null) {
            String s = dbStatus.toLowerCase();
            if (s.equals("borrowed") || s.equals("returned") || s.equals("overdue")) return s;
        }
        return "borrowed";
    }

    private String computeStatusSQL(Timestamp expected, Timestamp actual, String dbStatus, Date now) {
        if (actual != null) return "returned";
        if (expected != null && expected.before(now)) return "overdue";
        if (dbStatus != null) {
            String s = dbStatus.toLowerCase();
            if (s.equals("borrowed") || s.equals("returned") || s.equals("overdue")) return s;
        }
        return "borrowed";
    }
}
