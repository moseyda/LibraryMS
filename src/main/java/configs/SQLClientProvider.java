package configs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLClientProvider {
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/dbLibraryMS";
    private static final String USER = "root";
    private static final String PASSWORD = "LibraryMS2026";

    private static volatile boolean driverLoaded = false;

    private SQLClientProvider() { /* utility */ }

    private static synchronized void ensureDriverLoaded() throws SQLException {
        if (driverLoaded) return;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found on runtime classpath", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        ensureDriverLoaded();
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}