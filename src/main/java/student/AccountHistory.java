// File: `src/main/java/student/AccountHistory.java`
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
import org.bson.json.JsonWriterSettings;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(name = "student.AccountHistory", value = "/accountHistory")
public class AccountHistory extends HttpServlet {

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "dbLibraryMS";
    private static final Set<String> ALLOWED_FIELDS =
            new HashSet<>(Arrays.asList("borrowDate", "expectedReturnDate", "actualReturnDate", "title", "status"));

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
        int sortDir = "asc".equalsIgnoreCase(dir) ? 1 : -1;

        try (MongoClient client = MongoClients.create(MONGO_URI);
             PrintWriter out = response.getWriter()) {

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
                        String computed = computeStatus(copy, now);
                        copy.put("status", computed);
                        return copy;
                    })
                    .collect(Collectors.toList());

            JsonWriterSettings settings = JsonWriterSettings.builder().build();
            String json = enriched.stream()
                    .map(doc -> doc.toJson(settings))
                    .collect(Collectors.joining(",", "[", "]"));

            response.setStatus(HttpServletResponse.SC_OK);
            out.write(json);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = e.getMessage() == null ? "Server error" : e.getMessage().replace("\"", "'");
            response.getWriter().write("{\"error\":\"" + msg + "\"}");
        }
    }

    private String computeStatus(Document doc, Date now) {
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
}
