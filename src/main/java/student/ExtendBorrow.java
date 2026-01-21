package student;

import admin.NotificationServlet;
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
import java.sql.*;
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

            Document borrowRecord = histCol.find(
                    new Document("_id", new ObjectId(borrowId))
                            .append("status", "borrowed")
            ).first();

            if (borrowRecord == null) {
                return new Object[]{false, "Borrow record not found or already returned"};
            }

            Date currentExpectedReturnDate = borrowRecord.getDate("expectedReturnDate");
            String SNumber = borrowRecord.getString("SNumber");
            String title = borrowRecord.getString("title");

            LocalDate newExpectedReturnDate = Instant.ofEpochMilli(
                            currentExpectedReturnDate.getTime()
                    ).atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .plusDays(1);

            histCol.updateOne(
                    new Document("_id", new ObjectId(borrowId)),
                    new Document("$set", new Document(
                            "expectedReturnDate",
                            Date.from(
                                    newExpectedReturnDate
                                            .atStartOfDay(ZoneId.systemDefault())
                                            .toInstant()
                            )
                    ))
            );
            // --- CREATE notification ---
            NotificationServlet.createNotification(
                    "extend",
                    "Borrow Extended",
                    "Student " + SNumber + " extended \"" + title + "\" by 1 day"
            );

            return new Object[]{true, null};

        } catch (Exception e) {
            return new Object[]{false, e.getMessage()};
        }
    }


    private Object[] extendBorrowSQL(String borrowId) {
        if (borrowId == null || borrowId.trim().isEmpty()) {
            return new Object[]{false, "Missing borrowId"};
        }

        boolean isNumeric = true;
        int numericId = -1;
        try {
            numericId = Integer.parseInt(borrowId);
        } catch (NumberFormatException ignored) {
            isNumeric = false;
        }

        try (Connection conn = SQLClientProvider.getConnection()) {
            try {
                conn.setAutoCommit(false);

                String selectSql =
                        "SELECT expectedReturnDate, SNumber, title " +
                                "FROM BorrowReturnHist " +
                                "WHERE record_id = ? AND status = 'borrowed'";

                Timestamp currentExpectedReturnDate;
                String SNumber;
                String title;


                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    if (isNumeric) selectStmt.setInt(1, numericId);
                    else selectStmt.setString(1, borrowId);

                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return new Object[]{false, "Borrow record not found or already returned"};
                        }
                        currentExpectedReturnDate = rs.getTimestamp("expectedReturnDate");
                        SNumber = rs.getString("SNumber");
                        title = rs.getString("title");
                        ;
                    }
                }

                // --- Calculate new expectedReturnDate (+1 day) ---
                Timestamp newExpectedReturnDate =
                        Timestamp.valueOf(
                                currentExpectedReturnDate
                                        .toLocalDateTime()
                                        .plusDays(1)
                        );

                // --- UPDATE record ---
                String updateSql =
                        "UPDATE BorrowReturnHist " +
                                "SET expectedReturnDate = ? " +
                                "WHERE record_id = ?";

                int updated;
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setTimestamp(1, newExpectedReturnDate);
                    if (isNumeric) updateStmt.setInt(2, numericId);
                    else updateStmt.setString(2, borrowId);
                    updated = updateStmt.executeUpdate();
                }

                if (updated == 0) {
                    conn.rollback();
                    return new Object[]{false, "Failed to update borrow record"};
                }

                conn.commit();
                NotificationServlet.createNotification(
                        "extend",
                        "Borrow Extended",
                        "Student " + SNumber + " extended \"" + title + "\" by 1 day"
                );


                return new Object[]{true, null};

            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ignore) {
                }
                return new Object[]{false, e.getMessage()};
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignore) {
                }
            }
        } catch (SQLException e) {
            return new Object[]{false, e.getMessage()};
        }
    }
}

