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
    private static DatabaseConfig databaseConfig;
    private static boolean enabled = false;

    /**
     * Establishes and returns a new connection to the specific game database.
     * * @return A valid {@link Connection} object linked to the MySQL database.
     *
     * @throws SQLException If a database access error occurs, the credentials are wrong,
     *                      or the MySQL server is unreachable.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(databaseConfig.url(), databaseConfig.user(), databaseConfig.password());
    }

    /**
     * Attempts connection to the target database.
     * If the database and the user exist, it creates the table if missing.
     * Otherwise, it throws an SQL exception that will be handled by the caller.
     */
    public static synchronized void initializeDatabase(DatabaseConfig dbConfig) throws SQLException {
        // Optimistic attempt: connects directly to the specific DB (e.g., Mesos_db)
        enabled = dbConfig.enabled();
        try (Connection conn = DriverManager.getConnection(dbConfig.url(), dbConfig.user(), dbConfig.password());
             Statement stmt = conn.createStatement()) {

            System.out.println("✓ Database connection successfully established.");

            // Table creation (Note: inserted "score INT" instead of "is_winner" to align with the specification)
            String createTableSQL = "CREATE TABLE IF NOT EXISTS match_archive ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "nickname VARCHAR(50) NOT NULL, "
                    + "score INT NOT NULL, "
                    + "player_count INT NOT NULL, "
                    + "match_day TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ");";

            stmt.execute(createTableSQL);
            System.out.println("✓ Table 'match_archive' verified/created.");
            databaseConfig = dbConfig;
        }
    }

    /**
     * Fallback method: uses admin credentials to create the database, the user, and grant permissions.
     */
    public static synchronized void setupEnvironmentWithRoot(String rootUser, String rootPassword, DatabaseConfig dbConfig) throws SQLException {
        // Extract the base URL (without the DB name) and the DB name
        int lastSlashIndex = dbConfig.url().lastIndexOf('/');
        String baseUrl = dbConfig.url().substring(0, lastSlashIndex + 1); // e.g., jdbc:mysql://localhost:3306/
        String dbName = dbConfig.url().substring(lastSlashIndex + 1);     // e.g., Mesos_db

        System.out.println("\n[SYSTEM] Starting environment configuration with administrator privileges...");

        // "Bare" connection using root credentials
        try (Connection rootConn = DriverManager.getConnection(baseUrl, rootUser, rootPassword);
             Statement stmt = rootConn.createStatement()) {

            // 1. Database creation
            stmt.execute("CREATE DATABASE IF NOT EXISTS " + dbName);
            System.out.println("  ✓ Database '" + dbName + "' created.");

            // 2. User creation (if it doesn't exist)
            stmt.execute("CREATE USER IF NOT EXISTS '" + dbConfig.user() + "'@'localhost' IDENTIFIED BY '" + dbConfig.password() + "'");
            System.out.println("  ✓ User '" + dbConfig.user() + "' created/verified.");

            // 3. Granting permissions
            stmt.execute("GRANT ALL PRIVILEGES ON " + dbName + ".* TO '" + dbConfig.user() + "'@'localhost'");
            System.out.println("  ✓ Permissions granted successfully.");

            databaseConfig = dbConfig;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void disable() {
        enabled = false;
    }
}
