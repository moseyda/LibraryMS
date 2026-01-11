package student;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import configs.DatabaseConfig;
import configs.SQLClientProvider;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

        try (PrintWriter out = response.getWriter()) {
            Object[] result;
            if (DatabaseConfig.isMongoDB()) {
                result = extendBorrowMongoDB(borrowId);
            } else {
                result = extendBorrowSQL(borrowId);
            }

            boolean success = (boolean) result[0];
            String errorMsg = (String) result[1];

            if (errorMsg != null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\":\"" + errorMsg + "\"}");
            } else if (success) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.write("{\"success\":true,\"message\":\"Borrow period extended by 1 day\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("{\"error\":\"Failed to extend borrow period\"}");
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private Object[] extendBorrowMongoDB(String borrowId) {
        try (MongoClient client = MongoClients.create(MONGO_URI)) {
            MongoDatabase db = client.getDatabase(DB_NAME);
            MongoCollection<Document> histCol = db.getCollection("BorrowReturnHist");

            Document borrowRecord = histCol.find(new Document("_id", new ObjectId(borrowId))).first();
            if (borrowRecord == null) {
                return new Object[]{false, "Borrow record not found"};
            }

            Date currentExpectedReturnDate = borrowRecord.getDate("expectedReturnDate");
            LocalDate newExpectedReturnDate = Instant.ofEpochMilli(currentExpectedReturnDate.getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .plusDays(1);

            histCol.updateOne(
                    new Document("_id", new ObjectId(borrowId)),
                    new Document("$set", new Document("expectedReturnDate",
                            Date.from(newExpectedReturnDate.atStartOfDay(ZoneId.systemDefault()).toInstant())))
            );

            return new Object[]{true, null};
        } catch (Exception e) {
            return new Object[]{false, e.getMessage()};
        }
    }

    private Object[] extendBorrowSQL(String borrowId) throws SQLException {
        try (Connection conn = SQLClientProvider.getConnection()) {
            // Get current expected return date
            String selectSql = "SELECT expectedReturnDate FROM BorrowReturnHist WHERE id = ?";
            java.sql.Date currentExpectedReturnDate;

            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setInt(1, Integer.parseInt(borrowId));
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    return new Object[]{false, "Borrow record not found"};
                }
                currentExpectedReturnDate = rs.getDate("expectedReturnDate");
            }

            // Calculate new expected return date (+1 day)
            LocalDate newExpectedReturnDate = currentExpectedReturnDate.toLocalDate().plusDays(1);

            // Update the record
            String updateSql = "UPDATE BorrowReturnHist SET expectedReturnDate = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setDate(1, java.sql.Date.valueOf(newExpectedReturnDate));
                stmt.setInt(2, Integer.parseInt(borrowId));
                stmt.executeUpdate();
            }

            return new Object[]{true, null};
        }
    }
}
