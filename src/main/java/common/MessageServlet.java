package common;

import java.io.IOException;
import java.util.Date;

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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String studentNumber = req.getParameter("studentNumber");
        String name = req.getParameter("name");
        String subject = req.getParameter("subject");
        String message = req.getParameter("message");

        try (MongoClient mongo = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase db = mongo.getDatabase("dbLibraryMS");
            MongoCollection<Document> students = db.getCollection("Students");
            MongoCollection<Document> messages = db.getCollection("Messages");

            // Validate student exists
            Document student = students.find(new Document("SNumber", studentNumber)).first();
            resp.setContentType("application/json");

            if (student == null) {
                resp.getWriter().write("{\"success\":false,\"error\":\"No such student found. Message sending failed\"}");
                return;
            }

            // Insert the message
            Document msg = new Document("studentNumber", studentNumber)
                    .append("name", name)
                    .append("subject", subject)
                    .append("message", message)
                    .append("timestamp", new Date());

            messages.insertOne(msg);
            resp.getWriter().write("{\"success\":true}");
        }
    }
}
