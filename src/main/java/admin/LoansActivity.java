package admin;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;

import java.io.PrintWriter;
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

        String statusFilter = request.getParameter("status"); // optional: borrowed / returned / overdue

        try (MongoClient client = MongoClients.create(MONGO_URI);
             PrintWriter out = response.getWriter()) {

            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<Document> col = db.getCollection("BorrowReturnHist");

            // Base filter: we do NOT filter by status here to allow computed overdue
            List<Document> records = new ArrayList<>();
            col.find(new Document())
                    .sort(new Document("borrowDate", -1))
                    .into(records);

            // Enrich with computed status
            Date now = new Date();
            List<Document> enriched = records.stream()
                    .map(doc -> {
                        Document copy = new Document(doc); // avoid mutating original
                        String computed = computeStatus(copy, now);
                        copy.put("status", computed); // override status field for frontend
                        return copy;
                    })
                    .filter(d -> statusFilter == null || statusFilter.isBlank()
                            || statusFilter.equalsIgnoreCase(d.getString("status")))
                    .collect(Collectors.toList());

            JsonWriterSettings settings = JsonWriterSettings.builder().build();
            String json = enriched.stream()
                    .map(d -> d.toJson(settings))
                    .collect(Collectors.joining(",", "[", "]"));

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

    private String computeStatus(Document doc, Date now) {
        Date expected = doc.getDate("expectedReturnDate");
        Date actual = doc.getDate("actualReturnDate");
        String dbStatus = doc.getString("status"); // original

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
