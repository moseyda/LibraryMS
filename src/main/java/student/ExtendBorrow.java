package student;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@WebServlet(name = "student.ExtendBorrow", value = "/extendBorrow")
public class ExtendBorrow extends HttpServlet {

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "dbLibraryMS";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        String borrowId = request.getParameter("borrowId");

        try (MongoClient client = MongoClients.create(MONGO_URI);
             PrintWriter out = response.getWriter()) {

            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<Document> histCol = db.getCollection("BorrowReturnHist");

            Document borrowRecord = histCol.find(new Document("_id", new ObjectId(borrowId))).first();
            if (borrowRecord == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\":\"Borrow record not found\"}");
                return;
            }

            Date currentExpectedReturnDate = borrowRecord.getDate("expectedReturnDate");
            LocalDate newExpectedReturnDate = Instant.ofEpochMilli(currentExpectedReturnDate.getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .plusDays(10);

            histCol.updateOne(
                    new Document("_id", new ObjectId(borrowId)),
                    new Document("$set", new Document("expectedReturnDate",
                            Date.from(newExpectedReturnDate.atStartOfDay(ZoneId.systemDefault()).toInstant())))
            );

            response.setStatus(HttpServletResponse.SC_OK);
            out.write("{\"success\":true,\"message\":\"Borrow period extended by 10 days\"}");

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
