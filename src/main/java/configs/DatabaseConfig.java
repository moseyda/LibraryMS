package configs;// THIS IS THE CONFIGURATION FILE TO SWITCH BETWEEN DATABASE BACKENDS
// Set DB_TYPE to "mongodb" for MongoDB backend or "sql" for SQL backend


public class DatabaseConfig {
    // Change this to switch backends
    public static final String DB_TYPE = "sql"; // or "sql"

    public static boolean isMongoDB() {
        return "mongodb".equalsIgnoreCase(DB_TYPE);
    }

    public static boolean isSQL() {
        return "sql".equalsIgnoreCase(DB_TYPE);
    }
}
