package common;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;


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
            writeJson(response, new Document("success", false).append("error", "Not authenticated"));
            return;
        }

        try (MongoClient mongo = MongoClients.create(DB_URL)) {
            MongoCollection<Document> history = mongo.getDatabase(DB_NAME).getCollection(HISTORY_COLLECTION);
            List<Document> overdueLoans = new ArrayList<>();
            Instant now = Instant.now();

            // Only still-borrowed, unpaid\-fine loans
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

            writeJson(response, new Document("success", true).append("overdueLoans", overdueLoans));
        } catch (Exception e) {
            writeJson(response,
                    new Document("success", false).append("error", "Failed to fetch overdue loans: " + e.getMessage()));
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
            writeJson(response, new Document("success", false).append("error", "Not authenticated"));
            return;
        }

        // Front\-end sends `loanIds[]`; support both for safety
        String[] selectedLoanIds = request.getParameterValues("loanIds[]");
        if (selectedLoanIds == null || selectedLoanIds.length == 0) {
            selectedLoanIds = request.getParameterValues("loanIds");
        }
        if (selectedLoanIds == null || selectedLoanIds.length == 0) {
            writeJson(response, new Document("success", false).append("error", "No loans selected"));
            return;
        }

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
                writeJson(response,
                        new Document("success", false).append("error", "No valid loans found for payment"));
                return;
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

            writeJson(response, new Document("success", true)
                    .append("receiptId", fineRecord.getObjectId("_id").toHexString())
                    .append("message", "Payment processed successfully"));
        } catch (Exception e) {
            writeJson(response,
                    new Document("success", false).append("error", "Failed to process payment: " + e.getMessage()));
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
            writeJson(response, new Document("success", false).append("error", "Not authenticated"));
            return;
        }

        try (MongoClient mongo = MongoClients.create(DB_URL)) {
            MongoCollection<Document> fines = mongo.getDatabase(DB_NAME).getCollection(FINES_COLLECTION);
            List<Document> paymentHistory = new ArrayList<>();

            fines.find(new Document("studentNumber", studentNumber))
                    .sort(new Document("createdAt", -1))
                    .into(paymentHistory);

            writeJson(response, new Document("success", true).append("payments", paymentHistory));
        } catch (Exception e) {
            writeJson(response,
                    new Document("success", false).append("error", "Failed to fetch payment history: " + e.getMessage()));
        }
    }

    // ---------------------------------------------------
    // /admin/getFinesActivity
    // ---------------------------------------------------
    private void getFinesActivity(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try (MongoClient mongo = MongoClients.create(DB_URL)) {
            MongoCollection<Document> fines = mongo.getDatabase(DB_NAME).getCollection(FINES_COLLECTION);
            List<Document> allFines = new ArrayList<>();

            fines.find().sort(new Document("createdAt", -1)).into(allFines);

            writeJson(response, new Document("success", true).append("fines", allFines));
        } catch (Exception e) {
            writeJson(response,
                    new Document("success", false).append("error", "Failed to fetch fines: " + e.getMessage()));
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
            writeJson(response, new Document("success", false).append("error", "Missing parameters"));
            return;
        }

        try {
            double newAmount = Double.parseDouble(newAmountStr);
            if (newAmount < 0) {
                writeJson(response, new Document("success", false).append("error", "Amount cannot be negative"));
                return;
            }

            try (MongoClient mongo = MongoClients.create(DB_URL)) {
                MongoCollection<Document> fines = mongo.getDatabase(DB_NAME).getCollection(FINES_COLLECTION);
                fines.updateOne(
                        new Document("_id", new ObjectId(fineId)),
                        new Document("$set", new Document("totalAmount", newAmount)
                                .append("adjustedByAdmin", true)
                                .append("adjustmentDate", LocalDateTime.now().toString())));

                writeJson(response, new Document("success", true).append("message", "Fine amount updated"));
            }
        } catch (NumberFormatException nfe) {
            writeJson(response, new Document("success", false).append("error", "Invalid amount format"));
        } catch (Exception e) {
            writeJson(response,
                    new Document("success", false).append("error", "Failed to update fine: " + e.getMessage()));
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

    private String formatDate(Date date) {
        return date == null
                ? null
                : ISO.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
    }
}
