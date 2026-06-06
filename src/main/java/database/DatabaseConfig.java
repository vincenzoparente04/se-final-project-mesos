package database;

/**
 *
 * @param enabled boolean flag to indicate if database connection is on
 * @param url db url
 * @param user db user
 * @param password user password
 */
public record DatabaseConfig(
        boolean enabled,
        String url,
        String user,
        String password
) {
}
