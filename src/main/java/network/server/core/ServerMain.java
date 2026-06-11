package network.server.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import database.DatabaseConfig;
import network.server.NetworkUtil;
import network.server.rmi.GameServerRemote;
import network.server.rmi.GameServerRemoteImpl;

/**
 * Entry point of the Mesos game server.
 * <p>
 * Boots two transport listeners that share a single {@link LobbyManager}:
 * an RMI registry (for clients connecting via Java RMI) and a TCP socket
 * acceptor (for clients connecting via the textual socket protocol).
 * The two listeners are independent: a failure in one does not prevent
 * the other from serving clients.
 * <p>
 * Uses fixed ports:
 * - Socket server: 9999
 * - RMI registry: 1099 (standard Java RMI port)
 *
 *  The {@code main} method does not return: after starting the RMI
 *           registry, control enters the socket acceptor's blocking
 *           {@code accept()} loop and remains there for the lifetime of the
 *           server process.
 */
public class ServerMain {

    private static final String RMI_SERVICE_NAME = "MesosGameServer";
    private static final int SOCKET_PORT = 9999;
    private static final int RMI_PORT = 1099;


    /**
     * Boots the server: acquires the database configuration, advertises a
     * LAN-reachable RMI hostname, starts the shared {@link LobbyManager}, installs
     * a shutdown hook, then starts the RMI registry and enters the socket accept
     * loop (which does not return).
     *
     * @param args ignored
     * @throws IOException if interactive configuration input cannot be read
     */
    public static void main(String[] args) throws IOException {
        DatabaseConfig dbConfig = acquireConfiguration();

        if (dbConfig.enabled()) {
            System.out.println("\nDb parameters saved");
            System.out.println("  Complete URL: " + dbConfig.url());
            System.out.println("  User:         " + dbConfig.user());

            boolean dbReady = setupDatabaseEnvironment(dbConfig);
            if (!dbReady) {
                System.err.println("Unable to start the database infrastructure. Starting server without database functionality."); // TODO: on failure, start the server anyway but without DB functionality
                database.DatabaseManager.disable(); // Disables DB functionality for the rest of the server lifecycle
            }

        } else {
            System.out.println("\nStarting server without database functionality.");
            database.DatabaseManager.disable();
        }

        // Advertise a LAN-reachable IP to remote RMI peers; without this the
        // exported stubs would carry 127.0.0.1 (default of getLocalHost()) and
        // remote clients would not be able to invoke them.
        String host = NetworkUtil.detectLocalIPv4();
        System.setProperty("java.rmi.server.hostname", host);
        System.out.println("RMI export hostname: " + host);

        LobbyManager lobbyManager = new LobbyManager();
        lobbyManager.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server…");
            lobbyManager.shutdown();
        }, "server-shutdown"));

        startRmiRegistry(lobbyManager);
        startSocketAcceptor(lobbyManager);
    }

    /**
     * Interactively prompts the user for database configuration.
     * Returns a DatabaseConfig with the selected or default parameters.
     */
    private static DatabaseConfig acquireConfiguration() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║  Mesos Game Server - Configuration    ║");
        System.out.println("╚═══════════════════════════════════════╝\n");

        boolean useDb = askDatabaseUsage(reader);

        if (!useDb) {
            return new DatabaseConfig(false, "", "", "");
        }

        System.out.println("\n--- Database settings ---");
        String url = askWithDefault(reader, "URL", "jdbc:mysql://localhost:3306/Mesos_db");
        String user = askWithDefault(reader, "User", "mesos_admin");
        String password = askWithDefault(reader, "Password", "PriParOrsPan");

        System.out.println("\n✓ configuration completed!");
        return new DatabaseConfig(true, url, user, password);
    }

    /**
     * Prompts for database usage with validation.
     */
    private static boolean askDatabaseUsage(BufferedReader reader) throws IOException {
        while (true) {
            System.out.print("Do you want to use database functionality? [y/n] (default: no): ");
            String input = reader.readLine().trim().toLowerCase();

            if (input.isEmpty() || input.equals("no") || input.equals("n")) {
                System.out.println("✓ Database: Disabled");
                return false;
            }

            if (input.equals("yes") || input.equals("y")) {
                System.out.println("✓ Database: Enabled");
                return true;
            }

            System.out.println("✗ Invalid choice. Retry.\n");
        }
    }

    /**
     * Prompts for a specific database parameter with a sensible default.
     */
    private static String askWithDefault(BufferedReader reader, String prompt, String defaultValue) throws IOException {
        while (true) {
            System.out.print("Insert " + prompt + " (default: " + defaultValue + "): ");
            String input = reader.readLine().trim();
            if (input.isEmpty()) {
                System.out.println("✓ " + prompt + ": " + defaultValue);
                return defaultValue;
            }
            return input;
        }
    }

    /**
     * Handles the "Happy Path" flow and the possible interactive fallback for DB creation.
     * Returns true if the environment is ready, false in case of a critical error.
     */
    private static boolean setupDatabaseEnvironment(DatabaseConfig dbConfig) {
        try {
            // 1. Optimistic Attempt
            database.DatabaseManager.initializeDatabase(dbConfig);
            return true;

        } catch (java.sql.SQLException e) {
            int errorCode = e.getErrorCode();
            String sqlState = e.getSQLState();

            // Communication error (e.g., MySQL not running)
            if ("08S01".equals(sqlState)) {
                System.err.println("\n✗ CRITICAL ERROR: Unable to contact MySQL.");
                System.err.println("  Details: Make sure the MySQL service is running on the correct port.");
                return false;
            }

            // 1049 (Unknown database) or 1045 (Access denied)
            if (errorCode == 1049 || errorCode == 1045) {
                System.out.println("\n⚠ Database environment not found or insufficient permissions.");
                System.out.println("  Initial configuration is required. Administrator privileges (e.g., root) will be requested.");

                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    String rootUser = askWithDefault(reader, "Admin User", "root");

                    System.out.print("Insert Admin Password: ");
                    String rootPass = reader.readLine().trim();

                    // 2. Fallback: Executes the setup with the provided privileges
                    database.DatabaseManager.setupEnvironmentWithRoot(rootUser, rootPass, dbConfig);

                    // 3. Retry standard initialization (it must work now)
                    database.DatabaseManager.initializeDatabase(dbConfig);
                    return true;

                } catch (Exception ex) {
                    System.err.println("\n✗ ERROR during administrative setup: " + ex.getMessage());
                    return false;
                }
            }

            // Other unhandled errors
            System.err.println("\n✗ Unexpected SQL ERROR: " + e.getMessage());
            return false;
        }
    }


    /**
     * Creates an RMI registry on the given port and binds the
     * {@link GameServerRemoteImpl} stub under {@value #RMI_SERVICE_NAME}.
     * If registry creation fails the error is logged and the server
     * continues to start the socket acceptor — RMI clients will be unable
     * to connect, but socket clients will work normally.
     */
    private static void startRmiRegistry(LobbyManager lobby) {
        try {
            Registry registry = LocateRegistry.createRegistry(ServerMain.RMI_PORT);
            GameServerRemote stub = new GameServerRemoteImpl(lobby);
            registry.rebind(RMI_SERVICE_NAME, stub);
            System.out.println("RMI registry on port " + ServerMain.RMI_PORT + " — service name: " + RMI_SERVICE_NAME);
        } catch (Exception e) {
            System.err.println("Failed to start RMI registry: " + e.getMessage());
        }
    }

    /**
     * Opens the TCP listening socket and enters the accept loop. For each
     * incoming connection spawns a per-connection thread that runs a
     * {@link ConnectionHandshaker}; the acceptor thread itself never performs
     * handshake I/O, so a slow or hostile client cannot stall new connections.
     * <p>
     * This method blocks for the lifetime of the server: it returns only if
     * the listening socket itself is closed or fails.
     */
    private static void startSocketAcceptor(LobbyManager lobby) {
        try (ServerSocket serverSocket = new ServerSocket(ServerMain.SOCKET_PORT)) {
            System.out.println("Socket server on port " + ServerMain.SOCKET_PORT + " — waiting for connections.");


            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Socket connection from " + client.getInetAddress());
                Thread t = new Thread(new ConnectionHandshaker(client, lobby), "socket-handshake-" + client.getPort());
                t.setDaemon(true);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Socket server error: " + e.getMessage());
        }
    }
}
