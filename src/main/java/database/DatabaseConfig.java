package database;

public record DatabaseConfig(
        boolean enabled,
        String url,
        String user,
        String password
) {
}
