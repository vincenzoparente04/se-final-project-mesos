package network.client;

import network.client.view.BoardRenderer;
import network.client.view.GameStateRenderer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;


public class ClientMainCli {

    // ─── Configuration Record ──────────────────────────────────

    /**
     * Holds the transport-level connection configuration entered by the user.
     * The player name is negotiated separately, after the transport is open.
     */
    private record ConnectionConfig(
        ConnectionProtocol transport,
        String host,
        int port
    ) {}

    // ─── Main Entry Point ──────────────────────────────────────

    /**
     * Entry point that collects configuration from the user interactively,
     * then starts the game with those settings.
     * No command-line arguments required.
     */
    public static void main(String[] args) throws Exception {
        // PHASE 1: Interactive transport configuration
        ConnectionConfig config = acquireConfiguration();

        System.out.println("\n✓ Configuration complete!");
        System.out.println("  Protocol: " + config.transport());
        System.out.println("  Server: " + config.host() + ":" + config.port());
        System.out.println();

        // PHASE 2: Connect (transport only), negotiate name, then start the loop
        startGame(config);
    }

    // ─── Configuration Acquisition ─────────────────────────────
    
    /**
     * Interactively prompts the user for connection configuration.
     * Returns a ConnectionConfig with validated parameters.
     */
    private static ConnectionConfig acquireConfiguration() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║  Mesos Game Client - Configuration    ║");
        System.out.println("╚═══════════════════════════════════════╝\n");
        
        // Select protocol (default: Socket)
        ConnectionProtocol transport = selectTransport(reader);
        
        // Enter host (default: localhost)
        System.out.print("\nEnter server host (default: localhost): ");
        String host = reader.readLine().trim();
        if (host.isEmpty()) {
            host = "localhost";
            System.out.println("✓ Host: " + host);
        } else {
            System.out.println("✓ Host: " + host);
        }
        
        // Enter port (default: 9999 for Socket, 1099 for RMI)
        int port = readPort(reader, transport);

        return new ConnectionConfig(transport, host, port);
    }
    
    /**
     * Prompts for connection protocol selection with Socket as default.
     */
    private static ConnectionProtocol selectTransport(BufferedReader reader) throws IOException {
        while (true) {
            System.out.println("Select connection protocol:");
            System.out.println("  1) Socket (default)");
            System.out.println("  2) RMI");
            System.out.print("\nChoice (1 or 2, press ENTER for Socket): ");
            
            String choice = reader.readLine().trim();
            
            // Default: Socket if input is empty
            if (choice.isEmpty()) {
                System.out.println("✓ Protocol: Socket");
                return ConnectionProtocol.SOCKET;
            }
            
            if (choice.equals("1")) {
                System.out.println("✓ Protocol: Socket");
                return ConnectionProtocol.SOCKET;
            }
            if (choice.equals("2")) {
                System.out.println("✓ Protocol: RMI");
                return ConnectionProtocol.RMI;
            }
            
            System.out.println("✗ Invalid choice. Try again.\n");
        }
    }
    
    /**
     * Prompts for server port with a sensible default based on protocol.
     */
    private static int readPort(BufferedReader reader, ConnectionProtocol transport) throws IOException {
        int defaultPort = transport == ConnectionProtocol.SOCKET ? 9999 : 1099;
        
        while (true) {
            System.out.print("Enter server port (default: " + defaultPort + "): ");
            String input = reader.readLine().trim();
            
            if (input.isEmpty()) {
                System.out.println("✓ Port: " + defaultPort);
                return defaultPort;
            }
            
            try {
                int port = Integer.parseInt(input);
                if (port > 0 && port < 65536) {
                    System.out.println("✓ Port: " + port);
                    return port;
                }
                System.out.println("✗ Port must be between 1 and 65535. Try again.\n");
            } catch (NumberFormatException e) {
                System.out.println("✗ Invalid port number. Try again.\n");
            }
        }
    }
    
    /**
     * Prompts for player name with validation (non-empty, max 20 chars).
     */
    private static String readPlayerName(BufferedReader reader) throws IOException {
        while (true) {
            System.out.print("\nEnter player name: ");
            String playerName = reader.readLine().trim();
            
            if (playerName.isEmpty()) {
                System.out.println("✗ Player name cannot be empty. Try again.");
                continue;
            }
            
            if (playerName.length() > 20) {
                System.out.println("✗ Player name too long (max 20 chars). Try again.");
                continue;
            }
            
            System.out.println("✓ Player: " + playerName);
            return playerName;
        }
    }

    // ─── Main Game Loop ────────────────────────────────────────
    
    /**
     * Starts the game with the given configuration.
     * <p>
     * Opens the transport, then loops on the player-name prompt until the
     * server accepts the name. Only after acceptance are the local game state,
     * listener, reader thread and heartbeat scheduler created and started.
     */
    private static void startGame(ConnectionConfig config) throws Exception {
        System.out.println("Connecting via " + config.transport() + " to " + config.host() + ":" + config.port() + "...");
        VirtualServer virtualServer = VirtualServerFactory.connect(
            config.transport(),
            config.host(),
            config.port()
        );
        System.out.println("✓ Connected!\n");

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        GameStateRenderer renderer = new BoardRenderer();

        String playerName;
        LocalGameState localState;
        ClientStateListenerCli listener;
        while (true) {
            playerName = readPlayerName(stdin);
            localState = new LocalGameState();
            listener = new ClientStateListenerCli(playerName, renderer);
            if (virtualServer.tryRegisterName(playerName, localState, listener)) {
                break;
            }
            System.out.println("✗ Name '" + playerName + "' is already taken on the server. Try a different one.");
        }

        virtualServer.start();

        System.out.println("Commands: " + helpLine());
        System.out.print("> ");

        String input;
        while ((input = stdin.readLine()) != null) {
            String trimmed = input.trim();
            if (trimmed.isEmpty()) {
                System.out.print("> ");
                continue;
            }

            if (trimmed.equalsIgnoreCase("quit")) break;

            if (trimmed.equalsIgnoreCase("state")) {
                listener.forceRefresh(localState);
            } else if (trimmed.equalsIgnoreCase("tribes")) {
                listener.printAllTribes(localState);
            } else if (!dispatch(virtualServer, trimmed)) {
                System.out.println("[?] Unknown command. " + helpLine());
            }
            System.out.print("> ");
        }

        virtualServer.close();
        System.out.println("Disconnected.");
    }

    // ─── Command Dispatcher ────────────────────────────────────
    
    /**
     * @implNote This is a simple command dispatcher that parses the first word as the command and the rest as an argument.
     * It calls the appropriate method on the VirtualServer proxy based on the command.
     * @param proxy virtual server associated to the client
     * @param input command string took from CLI
     * @return false if the command is unknown, true otherwise
     */
    private static boolean dispatch(VirtualServer proxy, String input) {
        String[] parts = input.split("\\s+", 2);
        String verb = parts[0].toLowerCase();
        String arg  = parts.length > 1 ? parts[1].trim() : "";

        switch (verb) {
            case "lobbies" -> proxy.sendListLobbies();
            case "create" -> {
                try {
                    proxy.sendCreateLobby(Integer.parseInt(arg));
                } catch (NumberFormatException e) {
                    System.out.println("[ERROR] create requires a number of players");
                }
            }
            case "join" -> {
                if (arg.isEmpty()) { System.out.println("[ERROR] join requires a lobby id"); return true; }
                proxy.sendJoinLobby(arg);
            }
            case "color" -> {
                if (arg.isEmpty()) { System.out.println("[ERROR] color requires a colour name"); return true; }
                proxy.sendChooseColor(arg.toUpperCase());
            }
            case "totem" -> {
                if (arg.isEmpty()) { System.out.println("[ERROR] totem requires a tile letter"); return true; }
                proxy.sendPlaceTotem(arg.toUpperCase().charAt(0));
            }
            case "draw" -> {
                try {
                    proxy.sendDrawCard(Integer.parseInt(arg));
                } catch (NumberFormatException e) {
                    System.out.println("[ERROR] Invalid card ID: \"" + arg + "\"");
                }
            }
            case "end" -> proxy.sendEndTurn();
            case "leave" -> proxy.sendLeaveCommand();
            default    -> { return false; }
        }
        return true;
    }

    // ─── Helpers ───────────────────────────────────────────────
    
    private static String helpLine() {
        return "lobbies | create <n> | join <id> | color <COLOR> | totem <LETTER> | draw <ID> | end | leave | state | tribes | quit";
    }
}
