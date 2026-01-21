package admin;

import com.mongodb.client.*;
import configs.DatabaseConfig;
import configs.SQLClientProvider;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.*;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

@WebServlet(urlPatterns = {
        "/admin/getNotifications",
        "/admin/markNotificationRead",
        "/admin/markAllNotificationsRead",
        "/admin/createNotification"
})
public class NotificationServlet extends HttpServlet {

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "dbLibraryMS";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            if (DatabaseConfig.isMongoDB()) {
                out.write(getNotificationsMongo());
            } else {
                out.write(getNotificationsSQL());
            }
        } catch (Exception e) {
            resp.setStatus(500);
            out.write("{\"success\":false,\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String path = req.getServletPath();

        try (PrintWriter out = resp.getWriter()) {
            if (path.endsWith("/markNotificationRead")) {
                String id = req.getParameter("id");
                markAsRead(id);
                out.write("{\"success\":true}");
            } else if (path.endsWith("/markAllNotificationsRead")) {
                markAllAsRead();
                out.write("{\"success\":true}");
            } else if (path.endsWith("/createNotification")) {
                String type = req.getParameter("type");
                String title = req.getParameter("title");
                String message = req.getParameter("message");
                createNotification(type, title, message);
                out.write("{\"success\":true}");
            } else {
                out.write("{\"success\":false,\"error\":\"Unknown endpoint\"}");
            }
        } catch (Exception e) {
            resp.setStatus(500);
            try (PrintWriter out = resp.getWriter()) {
                out.write("{\"success\":false,\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
            }
        }
    }


    private String getNotificationsMongo() {
        try (MongoClient client = MongoClients.create(MONGO_URI)) {
            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<Document> col = db.getCollection("Notifications");

            List<Document> docs = new ArrayList<>();
            col.find().sort(new Document("timestamp", -1)).limit(50).into(docs);

            JSONArray arr = new JSONArray();
            for (Document doc : docs) {
                JSONObject obj = new JSONObject();
                obj.put("id", doc.getObjectId("_id").toHexString());
                obj.put("type", doc.getString("type"));
                obj.put("title", doc.getString("title"));
                obj.put("message", doc.getString("message"));
                obj.put("timestamp", doc.getDate("timestamp").getTime());
                obj.put("read", doc.getBoolean("read", false));
                arr.put(obj);
            }

            return new JSONObject().put("success", true).put("notifications", arr).toString();
        }
    }

    private String getNotificationsSQL() throws SQLException {
        String sql = "SELECT * FROM Notifications ORDER BY timestamp DESC LIMIT 50";

        try (Connection conn = SQLClientProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            JSONArray arr = new JSONArray();
            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("id", rs.getInt("notification_id"));
                obj.put("type", rs.getString("type"));
                obj.put("title", rs.getString("title"));
                obj.put("message", rs.getString("message"));
                obj.put("timestamp", rs.getTimestamp("timestamp").getTime());
                obj.put("read", rs.getBoolean("is_read"));
                arr.put(obj);
            }

            return new JSONObject().put("success", true).put("notifications", arr).toString();
        }
    }

    private void markAsRead(String id) throws SQLException {
        if (DatabaseConfig.isMongoDB()) {
            try (MongoClient client = MongoClients.create(MONGO_URI)) {
                MongoDatabase db = client.getDatabase(DB_NAME);
                db.getCollection("Notifications").updateOne(
                        new Document("_id", new ObjectId(id)),
                        new Document("$set", new Document("read", true))
                );
            }
        } else {
            try (Connection conn = SQLClientProvider.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE Notifications SET is_read = TRUE WHERE notification_id = ?")) {
                ps.setInt(1, Integer.parseInt(id));
                ps.executeUpdate();
            }
        }
    }

    private void markAllAsRead() throws SQLException {
        if (DatabaseConfig.isMongoDB()) {
            try (MongoClient client = MongoClients.create(MONGO_URI)) {
                MongoDatabase db = client.getDatabase(DB_NAME);
                db.getCollection("Notifications").updateMany(
                        new Document(),
                        new Document("$set", new Document("read", true))
                );
            }
        } else {
            try (Connection conn = SQLClientProvider.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE Notifications SET is_read = TRUE")) {
                ps.executeUpdate();
            }
        }
    }

    public static void createNotification(String type, String title, String message) {
        try {
            if (DatabaseConfig.isMongoDB()) {
                try (MongoClient client = MongoClients.create(MONGO_URI)) {
                    MongoDatabase db = client.getDatabase(DB_NAME);
                    Document doc = new Document()
                            .append("type", type)
                            .append("title", title)
                            .append("message", message)
                            .append("timestamp", new Date())
                            .append("read", false);
                    db.getCollection("Notifications").insertOne(doc);
                }
            } else {
                try (Connection conn = SQLClientProvider.getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "INSERT INTO Notifications (type, title, message, timestamp, is_read) VALUES (?, ?, ?, NOW(), FALSE)")) {
                    ps.setString(1, type);
                    ps.setString(2, title);
                    ps.setString(3, message);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
