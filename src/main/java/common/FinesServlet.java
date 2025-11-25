package common;


import com.mongodb.client.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
    private static final double FINE_PER_BOOK = 10.0;

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
        }
    }

    private void getOverdueLoans(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession();
        String studentNumber = (String) session.getAttribute("userSNumber");

        try (MongoClient mongo = MongoClients.create(DB_URL)) {
            MongoDatabase database = mongo.getDatabase(DB_NAME);
            MongoCollection<Document> loansCollection = database.getCollection("Loans");
            MongoCollection<Document> booksCollection = database.getCollection("Books");

            List<Document> overdueLoans = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (Document loan : loansCollection.find(new Document("studentNumber", studentNumber)
                    .append("status", "borrowed"))) {

                LocalDateTime expectedReturn = LocalDateTime.parse(
                        loan.getString("expectedReturnDate"),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                );

                if (now.isAfter(expectedReturn)) {
                    Document book = booksCollection.find(
                            new Document("_id", new ObjectId(loan.getString("bookId")))
                    ).first();

                    if (book != null) {
                        Document overdueDoc = new Document()
                                .append("loanId", loan.getObjectId("_id").toString())
                                .append("bookId", loan.getString("bookId"))
                                .append("title", book.getString("title"))
                                .append("isbn", book.getString("isbn"))
                                .append("borrowedDate", loan.getString("borrowedDate"))
                                .append("expectedReturnDate", loan.getString("expectedReturnDate"))
                                .append("fine", FINE_PER_BOOK);

                        overdueLoans.add(overdueDoc);
                    }
                }
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(new Document("success", true)
                    .append("overdueLoans", overdueLoans)
                    .toJson());

        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void processFinePayment(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession();
        String studentNumber = (String) session.getAttribute("userSNumber");
        String firstName = (String) session.getAttribute("userFName");
        String lastName = (String) session.getAttribute("userLName");
        String fullName = firstName + " " + lastName;

        String[] selectedLoanIds = request.getParameterValues("loanIds[]");

        if (selectedLoanIds == null || selectedLoanIds.length == 0) {
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"error\":\"No loans selected\"}");
            return;
        }

        try (MongoClient mongo = MongoClients.create(DB_URL)) {
            MongoDatabase database = mongo.getDatabase(DB_NAME);
            MongoCollection<Document> loansCollection = database.getCollection("Loans");
            MongoCollection<Document> booksCollection = database.getCollection("Books");
            MongoCollection<Document> finesCollection = database.getCollection("Fines");

            List<Document> paidBooks = new ArrayList<>();
            LocalDateTime paymentTime = LocalDateTime.now();

            for (String loanId : selectedLoanIds) {
                Document loan = loansCollection.find(
                        new Document("_id", new ObjectId(loanId))
                ).first();

                if (loan != null) {
                    Document book = booksCollection.find(
                            new Document("_id", new ObjectId(loan.getString("bookId")))
                    ).first();

                    if (book != null) {
                        paidBooks.add(new Document()
                                .append("title", book.getString("title"))
                                .append("isbn", book.getString("isbn")));
                    }

                    // Mark loan as having paid fine
                    loansCollection.updateOne(
                            new Document("_id", new ObjectId(loanId)),
                            new Document("$set", new Document("finePaid", true))
                    );
                }
            }

            // Create fine payment record
            Document fineRecord = new Document()
                    .append("studentNumber", studentNumber)
                    .append("fullName", fullName)
                    .append("books", paidBooks)
                    .append("totalAmount", selectedLoanIds.length * FINE_PER_BOOK)
                    .append("status", "paid")
                    .append("expectedPaymentDate", paymentTime.minusDays(7).toString())
                    .append("actualPaymentDate", paymentTime.toString())
                    .append("createdAt", paymentTime.toString());

            finesCollection.insertOne(fineRecord);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(new Document("success", true)
                    .append("receiptId", fineRecord.getObjectId("_id").toString())
                    .append("message", "Payment processed successfully")
                    .toJson());

        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void getPaymentHistory(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession();
        String studentNumber = (String) session.getAttribute("userSNumber");

        try (MongoClient mongo = MongoClients.create(DB_URL)) {
            MongoDatabase database = mongo.getDatabase(DB_NAME);
            MongoCollection<Document> finesCollection = database.getCollection("Fines");

            List<Document> paymentHistory = new ArrayList<>();
            finesCollection.find(new Document("studentNumber", studentNumber))
                    .sort(new Document("createdAt", -1))
                    .into(paymentHistory);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(new Document("success", true)
                    .append("payments", paymentHistory)
                    .toJson());

        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void getFinesActivity(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try (MongoClient mongo = MongoClients.create(DB_URL)) {
            MongoDatabase database = mongo.getDatabase(DB_NAME);
            MongoCollection<Document> finesCollection = database.getCollection("Fines");

            List<Document> allFines = new ArrayList<>();
            finesCollection.find()
                    .sort(new Document("createdAt", -1))
                    .into(allFines);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(new Document("success", true)
                    .append("fines", allFines)
                    .toJson());

        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void updateFineAmount(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String fineId = request.getParameter("fineId");
        String newAmountStr = request.getParameter("newAmount");

        try {
            double newAmount = Double.parseDouble(newAmountStr);

            try (MongoClient mongo = MongoClients.create(DB_URL)) {
                MongoDatabase database = mongo.getDatabase(DB_NAME);
                MongoCollection<Document> finesCollection = database.getCollection("Fines");

                finesCollection.updateOne(
                        new Document("_id", new ObjectId(fineId)),
                        new Document("$set", new Document("totalAmount", newAmount)
                                .append("adjustedByAdmin", true)
                                .append("adjustmentDate", LocalDateTime.now().toString()))
                );

                response.setContentType("application/json");
                response.getWriter().write("{\"success\":true,\"message\":\"Fine amount updated\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
