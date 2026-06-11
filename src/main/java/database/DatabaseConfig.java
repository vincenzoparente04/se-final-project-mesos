package database;

/**
 * Configuration record for database connection parameters.
 * 
 * This immutable record encapsulates all settings required to establish a connection to the MySQL database,
 * including whether the database feature is enabled or disabled.
 *
 * @param enabled true if database persistence is enabled for this session, false otherwise
 * @param url the JDBC connection URL pointing to the target MySQL database
 * @param user the username for database authentication
 * @param password the password for database authentication
 */
public record DatabaseConfig(
        boolean enabled,
        String url,
        String user,
        String password
) {
}
