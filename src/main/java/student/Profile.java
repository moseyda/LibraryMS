package student;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.bson.Document;

import java.io.IOException;

@WebServlet(name = "Profile", urlPatterns = {
        "/student/profile",
        "/student/updatePassword"
})
public class Profile extends HttpServlet {

    private static final String DB_URL = "mongodb://localhost:27017";
    private static final String DB_NAME = "dbLibraryMS";
    private static final String STUDENTS_COLLECTION = "Students";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        String sNumber = session != null ? (String) session.getAttribute("userSNumber") : null;
        String fName = session != null ? (String) session.getAttribute("userFName") : null;
        String lName = session != null ? (String) session.getAttribute("userLName") : null;

        if (sNumber == null) {
            writeJson(response, new Document("success", false)
                    .append("error", "Not authenticated"));
            return;
        }

        String fullName = (fName == null ? "" : fName) + " " + (lName == null ? "" : lName);
        String status = "Active";

        Document body = new Document("success", true)
                .append("fullName", fullName.trim())
                .append("studentNumber", sNumber)
                .append("status", status);

        writeJson(response, body);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String path = request.getServletPath();
        if ("/student/updatePassword".equals(path)) {
            handlePasswordUpdate(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handlePasswordUpdate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        String sNumber = session != null ? (String) session.getAttribute("userSNumber") : null;
        if (sNumber == null) {
            writeJson(response, new Document("success", false)
                    .append("error", "Not authenticated"));
            return;
        }

        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        if (currentPassword == null || newPassword == null || confirmPassword == null ||
                currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            writeJson(response, new Document("success", false)
                    .append("error", "All fields are required"));
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            writeJson(response, new Document("success", false)
                    .append("error", "New passwords do not match"));
            return;
        }

        // Simple password rule example: at least 6 chars
        if (newPassword.length() < 6) {
            writeJson(response, new Document("success", false)
                    .append("error", "New password must be at least 6 characters"));
            return;
        }

        try (MongoClient mongo = MongoClients.create(DB_URL)) {
            MongoDatabase db = mongo.getDatabase(DB_NAME);
            MongoCollection<Document> students = db.getCollection(STUDENTS_COLLECTION);

            Document existing = students.find(new Document("SNumber", sNumber)
                    .append("Password", currentPassword)).first();

            if (existing == null) {
                writeJson(response, new Document("success", false)
                        .append("error", "Current password is incorrect"));
                return;
            }

            students.updateOne(
                    new Document("_id", existing.getObjectId("_id")),
                    new Document("$set", new Document("Password", newPassword))
            );

            writeJson(response, new Document("success", true)
                    .append("message", "Password updated successfully"));
        } catch (Exception e) {
            writeJson(response, new Document("success", false)
                    .append("error", "Failed to update password: " + e.getMessage()));
        }
    }

    private void writeJson(HttpServletResponse response, Document body) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body.toJson());
    }
}
