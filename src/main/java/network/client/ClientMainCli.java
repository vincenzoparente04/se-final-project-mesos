package network.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;


public class ClientMainCli {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            printUsage();
            return;
        }

        String firstArg = args[0].toLowerCase();
        ConnectionProtocol transport;
        int port;
        String host;
        String playerName;
        if (firstArg.equals("socket") || firstArg.equals("rmi")) {
            if (args.length < 4) { printUsage(); return; }
            transport = ConnectionProtocol.from(args[0]);
            host = args[1];
            port = Integer.parseInt(args[2]);
            playerName = args[3];
        } else {
            transport = ConnectionProtocol.SOCKET;
            host = args[0];
            port = Integer.parseInt(args[1]);
            playerName = args[2];
        }

        LocalGameState localState = new LocalGameState();
        ClientStateListener listener = new ClientStateListenerCli();

        System.out.println("Connecting via " + transport + " to " + host + ":" + port + " as \"" + playerName + "\"...");

        VirtualServer proxy = VirtualServerFactory.create(transport, host, port, playerName, localState, listener);

        System.out.println("Connected. Use 'lobbies' to list lobbies, 'create <n>' or 'join <id>'.");
        printHelp();
        System.out.print("> ");

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String input;
        while ((input = stdin.readLine()) != null) {
            String trimmed = input.trim();
            if (trimmed.isEmpty()) {
                System.out.print("> ");
                continue;
            }

            if (trimmed.equalsIgnoreCase("quit")) break;

            if (trimmed.equalsIgnoreCase("state")) {
                ClientStateListenerCli.printState(localState);
            } else if (!dispatch(proxy, trimmed)) {
                System.out.println("[?] Unknown command. " + helpLine());
            }
            System.out.print("> ");
        }

        proxy.close();
        System.out.println("Disconnected.");
    }

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

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  ClientMainCli socket <host> <port>    <playerName>");
        System.err.println("  ClientMainCli rmi    <host> <rmiPort> <playerName>");
    }

    private static void printHelp() {
        System.out.println("Commands: " + helpLine());
    }

    private static String helpLine() {
        return "lobbies | create <n> | join <id> | color <COLOR> | totem <LETTER> | draw <ID> | end | leave | state | quit";
    }
}
