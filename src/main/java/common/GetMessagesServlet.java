package common;

import com.mongodb.client.*;
import org.bson.Document;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import java.text.SimpleDateFormat;

@WebServlet(name="GetMessages", value="/common/getMessages")
public class GetMessagesServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase db = mongo.getDatabase("dbLibraryMS");
            MongoCollection<Document> messages = db.getCollection("Messages");

            List<Document> docs = messages.find().sort(new Document("timestamp", -1)).into(new ArrayList<>());

            StringBuilder json = new StringBuilder();
            json.append("{\"success\":true,\"messages\":[");

            boolean first = true;
            for (Document doc : docs) {
                if (!first) json.append(",");
                first = false;

                String studentNumber = doc.getString("studentNumber") != null ? doc.getString("studentNumber") : "";
                String name = doc.getString("name") != null ? doc.getString("name") : "";
                String subject = doc.getString("subject") != null ? doc.getString("subject") : "";
                String message = doc.getString("message") != null ? doc.getString("message") : "";
                Date ts = doc.getDate("timestamp");
                long timestamp = ts != null ? ts.getTime() : 0;

                // Escape quotes in strings
                studentNumber = studentNumber.replace("\"", "\\\"");
                name = name.replace("\"", "\\\"");
                subject = subject.replace("\"", "\\\"");
                message = message.replace("\"", "\\\"");

                json.append("{")
                        .append("\"studentNumber\":\"").append(studentNumber).append("\",")
                        .append("\"name\":\"").append(name).append("\",")
                        .append("\"subject\":\"").append(subject).append("\",")
                        .append("\"message\":\"").append(message).append("\",")
                        .append("\"timestamp\":").append(timestamp)
                        .append("}");
            }

            json.append("]}");

            resp.getWriter().write(json.toString());

        } catch (Exception e) {
            String errorJson = "{\"success\":false,\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}";
            resp.getWriter().write(errorJson);
        }
    }
}
