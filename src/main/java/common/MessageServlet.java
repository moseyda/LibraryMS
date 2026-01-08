package common;

import java.io.IOException;
import java.sql.*;
import java.util.Date;

import configs.DatabaseConfig;
import configs.SQLClientProvider;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;

@WebServlet(name="SendMessage", value="/common/sendMessage")
public class MessageServlet extends HttpServlet {
    private static final String DB_URL = "mongodb://localhost:27017";
    private static final String DB_NAME = "dbLibraryMS";

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String studentNumber = req.getParameter("studentNumber");
        String name = req.getParameter("name");
        String subject = req.getParameter("subject");
        String message = req.getParameter("message");

        resp.setContentType("application/json");

        try {
            String json;
            if (DatabaseConfig.isMongoDB()) {
                json = sendMessageMongoDB(studentNumber, name, subject, message);
            } else {
                json = sendMessageSQL(studentNumber, name, subject, message);
            }
            resp.getWriter().write(json);
        } catch (Exception e) {
            resp.getWriter().write("{\"success\":false,\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private String sendMessageMongoDB(String studentNumber, String name, String subject, String message) {
        try (MongoClient mongo = MongoClients.create(DB_URL)) {
            MongoDatabase db = mongo.getDatabase(DB_NAME);
            MongoCollection<Document> students = db.getCollection("Students");
            MongoCollection<Document> messages = db.getCollection("Messages");

            // Validate student exists
            Document student = students.find(new Document("SNumber", studentNumber)).first();
            if (student == null) {
                return "{\"success\":false,\"error\":\"No such student found. Message sending failed\"}";
            }

            // Insert the message
            Document msg = new Document("studentNumber", studentNumber)
                    .append("name", name)
                    .append("subject", subject)
                    .append("message", message)
                    .append("timestamp", new Date());

            messages.insertOne(msg);
            return "{\"success\":true}";
        }
    }

    private String sendMessageSQL(String studentNumber, String name, String subject, String message) throws SQLException {
        try (Connection conn = SQLClientProvider.getConnection()) {
            // Validate student exists
            String checkSql = "SELECT SNumber FROM Students WHERE SNumber = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, studentNumber);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    return "{\"success\":false,\"error\":\"No such student found. Message sending failed\"}";
                }
            }

            // Insert the message
            String insertSql = "INSERT INTO Messages (studentNumber, name, subject, message, timestamp) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, studentNumber);
                insertStmt.setString(2, name);
                insertStmt.setString(3, subject);
                insertStmt.setString(4, message);
                insertStmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                insertStmt.executeUpdate();
            }

            return "{\"success\":true}";
        }
    }
}
