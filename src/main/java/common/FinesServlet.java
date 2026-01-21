package common;

import admin.NotificationServlet;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import configs.DatabaseConfig;
import configs.SQLClientProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;


@MultipartConfig
@WebServlet(name = "FinesServlet", urlPatterns = {
        "/student/getOverdueLoans",
        "/student/processFinePayment",
        "/student/getPaymentHistory",
        "/admin/getFinesActivity",
        "/admin/updateFineAmount"
})

public class FinesServlet extends HttpServlet {
    private static final String DB_URL = "mongodb://localhost:27017";
    private static final String DB_NAME = "dbLibraryMS";
    private static final String HISTORY_COLLECTION = "BorrowReturnHist";
    private static final String FINES_COLLECTION = "Fines";
    private static final double FINE_PER_BOOK = 10.0;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        switch (path) {
            case "/student/getOverdueLoans":
                getOverdueLoans(request, response);
                break;
            case "/student/getPaymentHistory":
                getPaymentHistory(request, response);
                break;
            case "/admin/getFinesActivity":
                getFinesActivity(request, response);
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        switch (path) {
            case "/student/processFinePayment":
                processFinePayment(request, response);
                break;
            case "/admin/updateFineAmount":
                updateFineAmount(request, response);
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ---------------------------------------------------
    // /student/getOverdueLoans
    // ---------------------------------------------------
    private void getOverdueLoans(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        String studentNumber = session != null ? (String) session.getAttribute("userSNumber") : null;
        if (studentNumber == null) {
            writeJson(response, "{\"success\":false,\"error\":\"Not authenticated\"}");
            return;
        }

        try {
            String json;
            if (DatabaseConfig.isMongoDB()) {
                json = getOverdueLoansMongoDBInternal(studentNumber);
            } else {
                json = getOverdueLoansSQLInternal(studentNumber);
            }
            writeJson(response, json);
        } catch (Exception e) {
            writeJson(response, "{\"success\":false,\"error\":\"Failed to fetch overdue loans: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private String getOverdueLoansMongoDBInternal(String studentNumber) {
        try (MongoClient mongo = MongoClients.create(DB_URL)) {
            MongoCollection<Document> history = mongo.getDatabase(DB_NAME).getCollection(HISTORY_COLLECTION);
            List<Document> overdueLoans = new ArrayList<>();
            Instant now = Instant.now();

            FindIterable<Document> cursor = history.find(new Document("SNumber", studentNumber)
                    .append("status", "borrowed")
                    .append("$or", Arrays.asList(
                            new Document("finePaid", new Document("$exists", false)),
                            new Document("finePaid", false))));

            for (Document loan : cursor) {
                Date expectedReturn = loan.getDate("expectedReturnDate");
                if (expectedReturn != null && now.isAfter(expectedReturn.toInstant())) {
                    Document overdueDoc = new Document()
                            .append("loanId", loan.getObjectId("_id").toHexString())
                            .append("title", loan.getString("title"))
                            .append("isbn", loan.getString("isbn"))
                            .append("borrowedDate", formatDate(loan.getDate("borrowDate")))
                            .append("expectedReturnDate", formatDate(expectedReturn))
                            .append("fine", FINE_PER_BOOK);
                    overdueLoans.add(overdueDoc);
                }
            }

            return new Document("success", true).append("overdueLoans", overdueLoans).toJson();
        }
    }

    private String getOverdueLoansSQLInternal(String studentNumber) throws SQLException {
        String sql = "SELECT h.record_id, h.borrowDate, h.expectedReturnDate, b.title, b.isbn " +
                "FROM BorrowReturnHist h " +
                "JOIN Books b ON h.book_id = b.book_id " +
                "WHERE h.SNumber = ? AND h.status = 'borrowed' " +
                "AND (h.finePaid IS NULL OR h.finePaid = FALSE) " +
                "AND h.expectedReturnDate < NOW()";

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentNumber);
            ResultSet rs = stmt.executeQuery();

            JSONArray overdueLoans = new JSONArray();
            while (rs.next()) {
                JSONObject loan = new JSONObject();
                loan.put("loanId", String.valueOf(rs.getInt("record_id")));
                loan.put("title", rs.getString("title"));
                loan.put("isbn", rs.getString("isbn"));
                loan.put("borrowedDate", formatTimestamp(rs.getTimestamp("borrowDate")));
                loan.put("expectedReturnDate", formatTimestamp(rs.getTimestamp("expectedReturnDate")));
                loan.put("fine", FINE_PER_BOOK);
                overdueLoans.put(loan);
            }

            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("overdueLoans", overdueLoans);
            return result.toString();
        }
    }

    // ---------------------------------------------------
    // /student/processFinePayment
    // ---------------------------------------------------
    private void processFinePayment(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        String studentNumber = session != null ? (String) session.getAttribute("userSNumber") : null;
        String firstName = session != null ? (String) session.getAttribute("userFName") : null;
        String lastName = session != null ? (String) session.getAttribute("userLName") : null;

        if (studentNumber == null || firstName == null || lastName == null) {
            writeJson(response, "{\"success\":false,\"error\":\"Not authenticated\"}");
            return;
        }

        String[] selectedLoanIds = request.getParameterValues("loanIds[]");
        if (selectedLoanIds == null || selectedLoanIds.length == 0) {
            selectedLoanIds = request.getParameterValues("loanIds");
        }
        if (selectedLoanIds == null || selectedLoanIds.length == 0) {
            writeJson(response, "{\"success\":false,\"error\":\"No loans selected\"}");
            return;
        }

        try {
            String json;
            if (DatabaseConfig.isMongoDB()) {
                json = processFinePaymentMongoDBInternal(studentNumber, firstName, lastName, selectedLoanIds);
            } else {
                json = processFinePaymentSQLInternal(studentNumber, firstName, lastName, selectedLoanIds);
            }
            writeJson(response, json);
        } catch (Exception e) {
            writeJson(response, "{\"success\":false,\"error\":\"Failed to process payment: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private String processFinePaymentMongoDBInternal(String studentNumber, String firstName, String lastName, String[] selectedLoanIds) {
        try (MongoClient mongo = MongoClients.create(DB_URL)) {
            MongoDatabase db = mongo.getDatabase(DB_NAME);
            MongoCollection<Document> history = db.getCollection(HISTORY_COLLECTION);
            MongoCollection<Document> fines = db.getCollection(FINES_COLLECTION);

            List<Document> paidBooks = new ArrayList<>();
            LocalDateTime paymentTime = LocalDateTime.now();

            for (String loanId : selectedLoanIds) {
                if (loanId == null || loanId.isBlank()) continue;

                Document loan = history.find(new Document("_id", new ObjectId(loanId))).first();
                if (loan != null) {
                    paidBooks.add(new Document()
                            .append("title", loan.getString("title"))
                            .append("isbn", loan.getString("isbn")));
                    history.updateOne(
                            new Document("_id", loan.getObjectId("_id")),
                            new Document("$set", new Document("finePaid", true)
                                    .append("finePaidDate", paymentTime.toString())));
                }
            }

            if (paidBooks.isEmpty()) {
                return "{\"success\":false,\"error\":\"No valid loans found for payment\"}";
            }

            double totalAmount = paidBooks.size() * FINE_PER_BOOK;

            Document fineRecord = new Document()
                    .append("studentNumber", studentNumber)
                    .append("fullName", firstName + " " + lastName)
                    .append("books", paidBooks)
                    .append("totalAmount", totalAmount)
                    .append("status", "paid")
                    .append("expectedPaymentDate", paymentTime.minusDays(7).toString())
                    .append("actualPaymentDate", paymentTime.toString())
                    .append("createdAt", paymentTime.toString());

            fines.insertOne(fineRecord);

            // --- Notification ---
            String studentName = firstName + " " + lastName;
            NotificationServlet.createNotification(
                    "fine",
                    "Fine Paid",
                    studentName + " paid £" + totalAmount + " in fines"
            );

            return new Document("success", true)
                    .append("receiptId", fineRecord.getObjectId("_id").toHexString())
                    .append("message", "Payment processed successfully").toJson();
        }
    }

    private String processFinePaymentSQLInternal(String studentNumber, String firstName, String lastName, String[] selectedLoanIds) throws SQLException {
        try (Connection conn = SQLClientProvider.getConnection()) {
            conn.setAutoCommit(false);

            List<JSONObject> paidBooks = new ArrayList<>();
            LocalDateTime paymentTime = LocalDateTime.now();

            String selectSql = "SELECT h.record_id, b.title, b.isbn FROM BorrowReturnHist h " +
                    "JOIN Books b ON h.book_id = b.book_id WHERE h.record_id = ?";
            String updateSql = "UPDATE BorrowReturnHist SET finePaid = TRUE, finePaidDate = ? WHERE record_id = ?";

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql);
                 PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

                for (String loanId : selectedLoanIds) {
                    if (loanId == null || loanId.isBlank()) continue;

                    selectStmt.setInt(1, Integer.parseInt(loanId));
                    ResultSet rs = selectStmt.executeQuery();

                    if (rs.next()) {
                        JSONObject book = new JSONObject();
                        book.put("title", rs.getString("title"));
                        book.put("isbn", rs.getString("isbn"));
                        paidBooks.add(book);

                        updateStmt.setTimestamp(1, Timestamp.valueOf(paymentTime));
                        updateStmt.setInt(2, Integer.parseInt(loanId));
                        updateStmt.executeUpdate();
                    }
                }
            }

            if (paidBooks.isEmpty()) {
                conn.rollback();
                return "{\"success\":false,\"error\":\"No valid loans found for payment\"}";
            }

            double totalAmount = paidBooks.size() * FINE_PER_BOOK;

            String insertFineSql = "INSERT INTO Fines (SNumber, fullName, totalAmount, status, " +
                    "expectedPaymentDate, actualPaymentDate, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement insertStmt = conn.prepareStatement(insertFineSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, studentNumber);
                insertStmt.setString(2, firstName + " " + lastName);
                insertStmt.setDouble(3, totalAmount);
                insertStmt.setString(4, "paid");
                insertStmt.setTimestamp(5, Timestamp.valueOf(paymentTime.minusDays(7)));
                insertStmt.setTimestamp(6, Timestamp.valueOf(paymentTime));
                insertStmt.setTimestamp(7, Timestamp.valueOf(paymentTime));
                insertStmt.executeUpdate();

                ResultSet keys = insertStmt.getGeneratedKeys();
                String receiptId = keys.next() ? String.valueOf(keys.getInt(1)) : "0";

                conn.commit();

                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("receiptId", receiptId);
                result.put("message", "Payment processed successfully");

                // --- Notification code ---
                String studentName = firstName + " " + lastName;
                NotificationServlet.createNotification(
                        "fine",
                        "Fine Paid",
                        studentName + " paid £" + totalAmount + " in fines"
                );

                return result.toString();
            }
        }
    }

    // ---------------------------------------------------
    // /student/getPaymentHistory
    // ---------------------------------------------------
    private void getPaymentHistory(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        String studentNumber = session != null ? (String) session.getAttribute("userSNumber") : null;
        if (studentNumber == null) {
            writeJson(response, "{\"success\":false,\"error\":\"Not authenticated\"}");
            return;
        }

        try {
            String json;
            if (DatabaseConfig.isMongoDB()) {
                json = getPaymentHistoryMongoDBInternal(studentNumber);
            } else {
                json = getPaymentHistorySQLInternal(studentNumber);
            }
            writeJson(response, json);
        } catch (Exception e) {
            writeJson(response, "{\"success\":false,\"error\":\"Failed to fetch payment history: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private String getPaymentHistoryMongoDBInternal(String studentNumber) {
        try (MongoClient mongo = MongoClients.create(DB_URL)) {
            MongoCollection<Document> fines = mongo.getDatabase(DB_NAME).getCollection(FINES_COLLECTION);
            List<Document> paymentHistory = new ArrayList<>();

            fines.find(new Document("studentNumber", studentNumber))
                    .sort(new Document("createdAt", -1))
                    .into(paymentHistory);

            return new Document("success", true).append("payments", paymentHistory).toJson();
        }
    }

    private String getPaymentHistorySQLInternal(String studentNumber) throws SQLException {
        String sql = "SELECT * FROM Fines WHERE SNumber = ? ORDER BY createdAt DESC";

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentNumber);
            ResultSet rs = stmt.executeQuery();

            JSONArray payments = new JSONArray();
            while (rs.next()) {
                JSONObject payment = new JSONObject();
                payment.put("_id", String.valueOf(rs.getInt("fine_id")));
                payment.put("studentNumber", rs.getString("SNumber"));
                payment.put("fullName", rs.getString("fullName"));
                payment.put("totalAmount", rs.getDouble("totalAmount"));
                payment.put("status", rs.getString("status"));
                payment.put("expectedPaymentDate", formatTimestamp(rs.getTimestamp("expectedPaymentDate")));
                payment.put("actualPaymentDate", formatTimestamp(rs.getTimestamp("actualPaymentDate")));
                payment.put("createdAt", formatTimestamp(rs.getTimestamp("createdAt")));
                payments.put(payment);
            }

            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("payments", payments);
            return result.toString();
        }
    }

    // ---------------------------------------------------
    // /admin/getFinesActivity
    // ---------------------------------------------------
    private void getFinesActivity(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            String json;
            if (DatabaseConfig.isMongoDB()) {
                json = getFinesActivityMongoDBInternal();
            } else {
                json = getFinesActivitySQLInternal();
            }
            writeJson(response, json);
        } catch (Exception e) {
            writeJson(response, "{\"success\":false,\"error\":\"Failed to fetch fines: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private String getFinesActivityMongoDBInternal() {
        try (MongoClient mongo = MongoClients.create(DB_URL)) {
            MongoCollection<Document> fines = mongo.getDatabase(DB_NAME).getCollection(FINES_COLLECTION);
            List<Document> allFines = new ArrayList<>();

            fines.find().sort(new Document("createdAt", -1)).into(allFines);

            return new Document("success", true).append("fines", allFines).toJson();
        }
    }

    private String getFinesActivitySQLInternal() throws SQLException {
        String sql = "SELECT * FROM Fines ORDER BY createdAt DESC";

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            JSONArray fines = new JSONArray();
            while (rs.next()) {
                JSONObject fine = new JSONObject();
                fine.put("_id", String.valueOf(rs.getInt("fine_id")));
                fine.put("studentNumber", rs.getString("SNumber"));
                fine.put("fullName", rs.getString("fullName"));
                fine.put("totalAmount", rs.getDouble("totalAmount"));
                fine.put("status", rs.getString("status"));
                fine.put("expectedPaymentDate", formatTimestamp(rs.getTimestamp("expectedPaymentDate")));
                fine.put("actualPaymentDate", formatTimestamp(rs.getTimestamp("actualPaymentDate")));
                fine.put("createdAt", formatTimestamp(rs.getTimestamp("createdAt")));
                fine.put("adjustedByAdmin", rs.getBoolean("adjustedByAdmin"));
                fine.put("adjustmentDate", formatTimestamp(rs.getTimestamp("adjustmentDate")));
                fines.put(fine);
            }

            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("fines", fines);
            return result.toString();
        }
    }

    // ---------------------------------------------------
    // /admin/updateFineAmount
    // ---------------------------------------------------
    private void updateFineAmount(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String fineId = request.getParameter("fineId");
        String newAmountStr = request.getParameter("newAmount");

        if (fineId == null || fineId.isBlank() || newAmountStr == null || newAmountStr.isBlank()) {
            writeJson(response, "{\"success\":false,\"error\":\"Missing parameters\"}");
            return;
        }

        try {
            double newAmount = Double.parseDouble(newAmountStr);
            if (newAmount < 0) {
                writeJson(response, "{\"success\":false,\"error\":\"Amount cannot be negative\"}");
                return;
            }

            String json;
            if (DatabaseConfig.isMongoDB()) {
                json = updateFineAmountMongoDBInternal(fineId, newAmount);
            } else {
                json = updateFineAmountSQLInternal(fineId, newAmount);
            }
            writeJson(response, json);

        } catch (NumberFormatException nfe) {
            writeJson(response, "{\"success\":false,\"error\":\"Invalid amount format\"}");
        } catch (Exception e) {
            writeJson(response, "{\"success\":false,\"error\":\"Failed to update fine: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private String updateFineAmountMongoDBInternal(String fineId, double newAmount) {
        try (MongoClient mongo = MongoClients.create(DB_URL)) {
            MongoCollection<Document> fines = mongo.getDatabase(DB_NAME).getCollection(FINES_COLLECTION);
            fines.updateOne(
                    new Document("_id", new ObjectId(fineId)),
                    new Document("$set", new Document("totalAmount", newAmount)
                            .append("adjustedByAdmin", true)
                            .append("adjustmentDate", LocalDateTime.now().toString())));

            return "{\"success\":true,\"message\":\"Fine amount updated\"}";
        }
    }

    private String updateFineAmountSQLInternal(String fineId, double newAmount) throws SQLException {
        String sql = "UPDATE Fines SET totalAmount = ?, adjustedByAdmin = TRUE, adjustmentDate = ? WHERE fine_id = ?";

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newAmount);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(3, Integer.parseInt(fineId));

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                return "{\"success\":true,\"message\":\"Fine amount updated\"}";
            } else {
                return "{\"success\":false,\"error\":\"Fine not found\"}";
            }
        }
    }

    // ---------------------------------------------------
    // Helpers
    // ---------------------------------------------------
    private void writeJson(HttpServletResponse response, Document body) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body.toJson());
    }

    private void writeJson(HttpServletResponse response, String json) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

    private String formatDate(Date date) {
        return date == null
                ? null
                : ISO.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
    }

    private String formatTimestamp(Timestamp ts) {
        return ts == null ? null : ISO.format(ts.toLocalDateTime());
    }
}
