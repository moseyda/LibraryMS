package common;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import configs.DatabaseConfig;
import configs.SQLClientProvider;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@WebServlet(name = "GetMessages", value = "/common/getMessages")
public class GetMessagesServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            String json = DatabaseConfig.isMongoDB()
                    ? getMessagesMongoDB()
                    : getMessagesSQL();
            resp.getWriter().write(json);
        } catch (Exception e) {
            String errorJson = "{\"success\":false,\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}";
            resp.getWriter().write(errorJson);
        }
    }

    private String getMessagesMongoDB() {
        try (MongoClient mongo = MongoClientProvider.createClient()) {
            MongoDatabase db = mongo.getDatabase(MongoClientProvider.getDatabaseName());
            MongoCollection<Document> messages = db.getCollection("Messages");

            List<Document> docs = messages.find()
                    .sort(new Document("timestamp", -1))
                    .into(new ArrayList<>());

            return buildJson(docs);
        }
    }

    private String getMessagesSQL() throws SQLException {
        String sql = "SELECT studentNumber, name, subject, message, timestamp FROM Messages ORDER BY timestamp DESC";
        List<MessageRow> rows = new ArrayList<>();

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                MessageRow row = new MessageRow();
                row.studentNumber = rs.getString("studentNumber");
                row.name = rs.getString("name");
                row.subject = rs.getString("subject");
                row.message = rs.getString("message");
                row.timestamp = rs.getTimestamp("timestamp") != null ? rs.getTimestamp("timestamp").getTime() : 0;
                rows.add(row);
            }
        }

        // Convert rows to Document list to reuse builder
        List<Document> docs = new ArrayList<>();
        for (MessageRow r : rows) {
            docs.add(new Document()
                    .append("studentNumber", r.studentNumber)
                    .append("name", r.name)
                    .append("subject", r.subject)
                    .append("message", r.message)
                    .append("timestamp", new Date(r.timestamp)));
        }
        return buildJson(docs);
    }

    private String buildJson(List<Document> docs) {
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
        return json.toString();
    }

    private static class MessageRow {
        String studentNumber;
        String name;
        String subject;
        String message;
        long timestamp;
    }
}
