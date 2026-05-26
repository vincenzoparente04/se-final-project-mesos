package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class that manages database connections and initialization.
 * It provides a centralized access point to the MySQL server and ensures
 * the underlying schema and tables exist before the game server starts accepting requests.
 */
    public class DatabaseManager {
        private static String url;
        private static String user;
        private static String password;

    public DatabaseManager(DatabaseConfig dbConfig) {
        url = dbConfig.url();
        user = dbConfig.user();
        password = dbConfig.password();
    }

    /**
     * Establishes and returns a new connection to the specific game database.
     * * @return A valid {@link Connection} object linked to the MySQL database.
     * @throws SQLException If a database access error occurs, the credentials are wrong,
     * or the MySQL server is unreachable.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Bootstraps the database infrastructure.
     * Connects to the base MySQL server to automatically create the target database
     * if it is missing, and sets up the required "match_archive" table.
     * <p>
     * This method is synchronized to prevent race conditions if multiple server threads
     * attempt to initialize the database concurrently during startup.
     * <p>
     * Throws SQLException if the database connection or initialization fails,
     * allowing the caller to handle the error and terminate gracefully.
     *
     * @throws SQLException if a database access error occurs or initialization fails
     */
    public static synchronized void initializeDatabase(DatabaseConfig dbConfig) throws SQLException {
        // get the database name
        int lastSlashIndex = dbConfig.url().lastIndexOf('/');
        String dbName = "";
        if (lastSlashIndex != -1) {
            dbName = dbConfig.url().substring(lastSlashIndex + 1);
        }

        // Step 1: generic connection to database to check if present
        try (Connection conn = DriverManager.getConnection(dbConfig.url(), dbConfig.user(), dbConfig.password());
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS " + dbName);
            System.out.println("Database '" + dbName + "' verified/created.");
        }
        // Step 2: Specific connection to the DB just created
        String createTableSQL = "CREATE TABLE IF NOT EXISTS match_archive ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "nickname VARCHAR(50) NOT NULL, "
                + "is_winner BOOL NOT NULL, "
                + "player_count INT NOT NULL, "
                + "match_day TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ");";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Table 'match_archive' verified/created.");
        }
    }
}
