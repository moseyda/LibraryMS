package student;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.bson.Document;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@WebServlet(name = "student.BorrowBook", value = "/borrowBook")
public class BorrowBook extends HttpServlet {

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "dbLibraryMS";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        String isbn = request.getParameter("isbn");
        String title = request.getParameter("title");

        if (isbn == null || isbn.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"ISBN is required\"}");
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedIn") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Not logged in\"}");
            return;
        }

        String studentNumber = (String) session.getAttribute("userSNumber");
        String firstName = (String) session.getAttribute("userFName");
        String lastName = (String) session.getAttribute("userLName");

        try (MongoClient client = MongoClients.create(MONGO_URI);
             PrintWriter out = response.getWriter()) {

            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<Document> booksCol = db.getCollection("Books");
            MongoCollection<Document> histCol = db.getCollection("BorrowReturnHist");

            Document book = booksCol.find(new Document("isbn", isbn)).first();
            if (book == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\":\"Book not found\"}");
                return;
            }

            int available = book.getInteger("available", 0);
            if (available <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\":\"No copies available\"}");
                return;
            }

            LocalDate borrowDate = LocalDate.now();
            LocalDate expectedReturnDate = borrowDate.plusDays(1); // change it to 5, 10 or any other duration, for test purposes, keep at 1 for 24 hour timeframe.

            Document borrowRecord = new Document()
                    .append("isbn", isbn)
                    .append("title", title)
                    .append("SNumber", studentNumber)
                    .append("firstName", firstName)
                    .append("lastName", lastName)
                    .append("borrowDate", Date.from(borrowDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    .append("expectedReturnDate", Date.from(expectedReturnDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    .append("actualReturnDate", null)
                    .append("status", "borrowed");

            histCol.insertOne(borrowRecord);

            booksCol.updateOne(
                    new Document("isbn", isbn),
                    new Document("$inc", new Document("available", -1))
            );

            response.setStatus(HttpServletResponse.SC_OK);
            out.write("{\"success\":true,\"message\":\"Book borrowed successfully\"}");

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
