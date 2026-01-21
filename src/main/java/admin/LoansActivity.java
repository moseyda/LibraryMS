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

@WebServlet(name = "admin.LoansActivity", urlPatterns = {"/admin/loansActivity", "/admin/studentHistory"})
public class LoansActivity extends HttpServlet {

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "dbLibraryMS";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        String path = request.getServletPath();

        try (PrintWriter out = response.getWriter()) {
            String json;
            if (path.endsWith("/studentHistory")) {
                json = handleStudentHistory(request);
            } else {
                String statusFilter = request.getParameter("status");
                if (DatabaseConfig.isMongoDB()) {
                    json = getLoansActivityMongoDB(statusFilter);
                } else {
                    json = getLoansActivitySQL(statusFilter);
                }
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

    // Student-focused History handler
    private String handleStudentHistory(HttpServletRequest request) throws Exception {
        String studentNumber = request.getParameter("studentNumber");
        String month = request.getParameter("month");

        if (studentNumber == null || studentNumber.isBlank()) {
            return "{\"success\":false,\"error\":\"Student number required\"}";
        }

        if (DatabaseConfig.isMongoDB()) {
            return getStudentHistoryMongoDB(studentNumber, month);
        } else {
            return getStudentHistorySQL(studentNumber, month);
        }
    }

    private String getStudentHistorySQL(String studentNumber, String month) throws Exception {
        StringBuilder sql = new StringBuilder(
                "SELECT record_id, SNumber, isbn, title, firstName, lastName, book_id, " +
                        "borrowDate, expectedReturnDate, actualReturnDate, status " +
                        "FROM BorrowReturnHist WHERE SNumber = ?");

        if (month != null && !month.isBlank()) {
            sql.append(" AND DATE_FORMAT(borrowDate, '%Y-%m') = ?");
        }
        sql.append(" ORDER BY borrowDate DESC");

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            ps.setString(1, studentNumber);
            if (month != null && !month.isBlank()) {
                ps.setString(2, month);
            }

            JSONArray records = new JSONArray();
            JSONObject studentInfo = new JSONObject();
            Date now = new Date();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (studentInfo.isEmpty()) {
                        studentInfo.put("firstName", rs.getString("firstName"));
                        studentInfo.put("lastName", rs.getString("lastName"));
                        studentInfo.put("studentNumber", studentNumber);
                    }

                    Timestamp expectedReturn = rs.getTimestamp("expectedReturnDate");
                    Timestamp actualReturn = rs.getTimestamp("actualReturnDate");
                    String computedStatus = computeStatusSQL(expectedReturn, actualReturn, rs.getString("status"), now);

                    JSONObject rec = new JSONObject();
                    rec.put("recordId", rs.getInt("record_id"));
                    rec.put("title", rs.getString("title"));
                    rec.put("isbn", rs.getString("isbn"));
                    rec.put("borrowDate", rs.getTimestamp("borrowDate") != null ? rs.getTimestamp("borrowDate").getTime() : JSONObject.NULL);
                    rec.put("expectedReturnDate", expectedReturn != null ? expectedReturn.getTime() : JSONObject.NULL);
                    rec.put("actualReturnDate", actualReturn != null ? actualReturn.getTime() : JSONObject.NULL);
                    rec.put("status", computedStatus);
                    records.put(rec);
                }
            }

            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("student", studentInfo);
            result.put("records", records);
            return result.toString();
        }
    }

    private String getStudentHistoryMongoDB(String studentNumber, String month) {
        try (MongoClient client = MongoClients.create(MONGO_URI)) {
            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<Document> col = db.getCollection("BorrowReturnHist");

            Document query = new Document("SNumber", studentNumber);
            List<Document> records = new ArrayList<>();
            col.find(query).sort(new Document("borrowDate", -1)).into(records);

            Date now = new Date();
            JSONArray arr = new JSONArray();
            JSONObject studentInfo = new JSONObject();

            for (Document doc : records) {
                Date borrowDate = doc.getDate("borrowDate");
                if (month != null && !month.isBlank() && borrowDate != null) {
                    String docMonth = String.format("%tY-%tm", borrowDate, borrowDate);
                    if (!docMonth.equals(month)) continue;
                }

                if (studentInfo.isEmpty()) {
                    studentInfo.put("firstName", doc.getString("firstName"));
                    studentInfo.put("lastName", doc.getString("lastName"));
                    studentInfo.put("studentNumber", studentNumber);
                }

                String computedStatus = computeStatusMongo(doc, now);
                JSONObject rec = new JSONObject();
                rec.put("recordId", doc.getObjectId("_id").toHexString());
                rec.put("title", doc.getString("title"));
                rec.put("isbn", doc.getString("isbn"));
                rec.put("borrowDate", borrowDate != null ? borrowDate.getTime() : JSONObject.NULL);
                Date expected = doc.getDate("expectedReturnDate");
                Date actual = doc.getDate("actualReturnDate");
                rec.put("expectedReturnDate", expected != null ? expected.getTime() : JSONObject.NULL);
                rec.put("actualReturnDate", actual != null ? actual.getTime() : JSONObject.NULL);
                rec.put("status", computedStatus);
                arr.put(rec);
            }

            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("student", studentInfo);
            result.put("records", arr);
            return result.toString();
        }
    }

 // General Loans Activity handler

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

    private String getLoansActivitySQL(String statusFilter) throws Exception {
        String sql = "SELECT * FROM BorrowReturnHist ORDER BY borrowDate DESC";

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            JSONArray jsonArray = new JSONArray();
            Date now = new Date();

            while (rs.next()) {
                Timestamp expectedReturn = rs.getTimestamp("expectedReturnDate");
                Timestamp actualReturn = rs.getTimestamp("actualReturnDate");
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
