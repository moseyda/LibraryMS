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

        String status = request.getParameter("status"); // optional

        try (MongoClient client = MongoClients.create(MONGO_URI);
             PrintWriter out = response.getWriter()) {

            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<Document> col = db.getCollection("BorrowReturnHist");

            Document filter = new Document();
            if (status != null && !status.isBlank()) {
                filter.append("status", status);
            }

            List<Document> records = new ArrayList<>();
            col.find(filter).sort(new Document("borrowDate", -1)).into(records);

            JsonWriterSettings settings = JsonWriterSettings.builder().build();
            String json = records.stream()
                    .map(d -> d.toJson(settings))
                    .collect(Collectors.joining(",", "[", "]"));

            response.setStatus(HttpServletResponse.SC_OK);
            out.write(json);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = e.getMessage() == null ? "Server error" : e.getMessage().replace("\"","'");
            try (PrintWriter out = response.getWriter()) {
                out.write("{\"error\":\"" + msg + "\"}");
            } catch (Exception ignored) {}
        }
    }
}
