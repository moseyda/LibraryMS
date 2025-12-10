package common;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class MongoClientProvider {

    public static MongoClient createClient() {
        String uri = System.getenv("MONGODB_URI");
        if (uri == null || uri.isEmpty()) {
            String host = System.getenv("MONGODB_HOST") != null ? System.getenv("MONGODB_HOST") : "localhost";
            String port = System.getenv("MONGODB_PORT") != null ? System.getenv("MONGODB_PORT") : "27017";
            uri = "mongodb://" + host + ":" + port;
        }
        return MongoClients.create(uri);
    }

    public static String getDatabaseName() {
        String db = System.getenv("MONGODB_DATABASE");
        return (db != null && !db.isEmpty()) ? db : "dbLibraryMS";
    }
}
