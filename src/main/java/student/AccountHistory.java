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
import jakarta.servlet.http.HttpSession;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

@WebServlet(name = "student.AccountHistory", value = "/accountHistory")
public class AccountHistory extends HttpServlet {

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "dbLibraryMS";
    private static final Set<String> ALLOWED_FIELDS =
            new HashSet<>(Arrays.asList("borrowDate", "expectedReturnDate", "actualReturnDate", "title", "status"));

    // Map frontend field names to SQL column names
    private static final Map<String, String> SQL_FIELD_MAP = new HashMap<>();
    static {
        SQL_FIELD_MAP.put("borrowDate", "h.borrowDate");
        SQL_FIELD_MAP.put("expectedReturnDate", "h.expectedReturnDate");
        SQL_FIELD_MAP.put("actualReturnDate", "h.actualReturnDate");
        SQL_FIELD_MAP.put("title", "b.title");
        SQL_FIELD_MAP.put("status", "h.status");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedIn") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Not logged in\"}");
            return;
        }

        String studentNumber = (String) session.getAttribute("userSNumber");
        String sortField = Optional.ofNullable(request.getParameter("sort")).orElse("borrowDate");
        if (!ALLOWED_FIELDS.contains(sortField)) sortField = "borrowDate";
        String dir = Optional.ofNullable(request.getParameter("dir")).orElse("desc");

        try (PrintWriter out = response.getWriter()) {
            String json;
            if (DatabaseConfig.isMongoDB()) {
                json = getAccountHistoryMongoDB(studentNumber, sortField, dir);
            } else {
                json = getAccountHistorySQL(studentNumber, sortField, dir);
            }
            response.setStatus(HttpServletResponse.SC_OK);
            out.write(json);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = e.getMessage() == null ? "Server error" : e.getMessage().replace("\"", "'");
            response.getWriter().write("{\"error\":\"" + msg + "\"}");
        }
    }

    private String getAccountHistoryMongoDB(String studentNumber, String sortField, String dir) {
        int sortDir = "asc".equalsIgnoreCase(dir) ? 1 : -1;

        try (MongoClient client = MongoClients.create(MONGO_URI)) {
            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<Document> col = db.getCollection("BorrowReturnHist");

            List<Document> history = new ArrayList<>();
            col.find(new Document("SNumber", studentNumber))
                    .sort(new Document(sortField, sortDir))
                    .into(history);

            // Compute status including overdue
            Date now = new Date();
            List<Document> enriched = history.stream()
                    .map(doc -> {
                        Document copy = new Document(doc);
                        String computed = computeStatusMongo(copy, now);
                        copy.put("status", computed);
                        return copy;
                    })
                    .collect(Collectors.toList());

            JsonWriterSettings settings = JsonWriterSettings.builder().build();
            return enriched.stream()
                    .map(doc -> doc.toJson(settings))
                    .collect(Collectors.joining(",", "[", "]"));
        }
    }

    private String getAccountHistorySQL(String studentNumber, String sortField, String dir) throws SQLException {
        String sqlSortField = SQL_FIELD_MAP.getOrDefault(sortField, "h.borrowDate");
        String sortDirection = "asc".equalsIgnoreCase(dir) ? "ASC" : "DESC";

        String sql = "SELECT h.record_id, h.SNumber, h.borrowDate, h.expectedReturnDate, " +
                "h.actualReturnDate, h.status, h.firstName, h.lastName, " +
                "b.title, b.isbn, b.book_id " +
                "FROM BorrowReturnHist h " +
                "JOIN Books b ON h.book_id = b.book_id " +
                "WHERE h.SNumber = ? " +
                "ORDER BY " + sqlSortField + " " + sortDirection;

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentNumber);
            ResultSet rs = stmt.executeQuery();

            JSONArray history = new JSONArray();
            Date now = new Date();

            while (rs.next()) {
                JSONObject record = new JSONObject();
                record.put("_id", String.valueOf(rs.getInt("record_id")));
                record.put("SNumber", rs.getString("SNumber"));
                record.put("title", rs.getString("title"));
                record.put("isbn", rs.getString("isbn"));
                record.put("book_id", rs.getInt("book_id"));
                record.put("firstName", rs.getString("firstName"));
                record.put("lastName", rs.getString("lastName"));

                Timestamp borrowDate = rs.getTimestamp("borrowDate");
                Timestamp expectedReturnDate = rs.getTimestamp("expectedReturnDate");
                Timestamp actualReturnDate = rs.getTimestamp("actualReturnDate");

                record.put("borrowDate", formatTimestamp(borrowDate));
                record.put("expectedReturnDate", formatTimestamp(expectedReturnDate));
                record.put("actualReturnDate", formatTimestamp(actualReturnDate));

                // Compute status including overdue
                String dbStatus = rs.getString("status");
                String computedStatus = computeStatusSQL(expectedReturnDate, actualReturnDate, dbStatus, now);
                record.put("status", computedStatus);

                history.put(record);
            }

            return history.toString();
        }
    }

    private String computeStatusMongo(Document doc, Date now) {
        Date expected = doc.getDate("expectedReturnDate");
        Date actual = doc.getDate("actualReturnDate");
        String dbStatus = doc.getString("status");

        if (actual != null) {
            return "returned";
        }
        if (expected != null && expected.before(now)) {
            return "overdue";
        }
        if (dbStatus != null) {
            String s = dbStatus.toLowerCase();
            if (s.equals("borrowed") || s.equals("returned") || s.equals("overdue")) {
                return s;
            }
        }
        return "borrowed";
    }

    private String computeStatusSQL(Timestamp expected, Timestamp actual, String dbStatus, Date now) {
        if (actual != null) {
            return "returned";
        }
        if (expected != null && expected.before(now)) {
            return "overdue";
        }
        if (dbStatus != null) {
            String s = dbStatus.toLowerCase();
            if (s.equals("borrowed") || s.equals("returned") || s.equals("overdue")) {
                return s;
            }
        }
        return "borrowed";
    }

    private String formatTimestamp(Timestamp ts) {
        if (ts == null) return null;
        return ts.toInstant().toString();
    }
}
