package client;

import client.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Entry point unificato per il client a riga di comando (CLI).
 * <p>
 * <b>Usage</b>:
 * <pre>
 * ClientMainCli socket &lt;host&gt; &lt;port&gt;    &lt;playerName&gt;
 * ClientMainCli rmi    &lt;host&gt; &lt;rmiPort&gt; &lt;playerName&gt;
 * </pre>
 * Se il protocollo viene omesso, il client utilizza di default SOCKET.
 */
public class ClientMainCli {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            printUsage();
            return;
        }

        // Parsing degli argomenti riutilizzando la logica della GUI
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
            // Comportamento legacy/default (Socket)
            transport = ConnectionProtocol.SOCKET;
            host = args[0];
            port = Integer.parseInt(args[1]);
            playerName = args[2];
        }

        LocalGameState localState = new LocalGameState();
        ClientStateListener listener = new ClientStateListenerCli();

        System.out.println("Connecting via " + transport + " to " + host + ":" + port
                + " as \"" + playerName + "\"...");

        // Utilizzo del Factory Pattern: deleghiamo la creazione del proxy di rete
        // a una factory centralizzata. Il client non conosce i dettagli implementativi.
        VirtualServer proxy = VirtualServerFactory.create(
                transport, host, port, playerName, localState, listener);

        // Pattern Decorator / Wrapper: Il controller astrae l'interfaccia di rete
        ClientController controller = new ClientController(proxy);

        System.out.println("Connected. Waiting for other players...");
        printHelp();
        System.out.print("> ");

        // Loop principale di I/O
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
            } else if (!dispatch(controller, trimmed)) {
                System.out.println("[?] Unknown command. " + helpLine());
            }
            System.out.print("> ");
        }

        proxy.close();
        System.out.println("Disconnected.");
    }

    // ─────────────────────────────────────────────────────────
    // Command dispatch
    // ─────────────────────────────────────────────────────────

    private static boolean dispatch(ClientController controller, String input) {
        String[] parts = input.split("\\s+", 2);
        String verb = parts[0].toLowerCase();
        String arg  = parts.length > 1 ? parts[1].trim() : "";

        switch (verb) {
            case "color" -> {
                if (arg.isEmpty()) { System.out.println("[ERROR] color requires a colour name"); return true; }
                controller.onColorChosen(arg.toUpperCase());
            }
            case "totem" -> {
                if (arg.isEmpty()) { System.out.println("[ERROR] totem requires a tile letter"); return true; }
                controller.onTotemPlaced(arg.toUpperCase().charAt(0));
            }
            case "draw" -> {
                try {
                    controller.onCardDrawn(Integer.parseInt(arg));
                } catch (NumberFormatException e) {
                    System.out.println("[ERROR] Invalid card ID: \"" + arg + "\"");
                }
            }
            case "end" -> controller.onTurnEnded();
            default    -> { return false; }
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  ClientMainCli socket <host> <port>    <playerName>");
        System.err.println("  ClientMainCli rmi    <host> <rmiPort> <playerName>");
    }

    private static void printHelp() {
        System.out.println("Commands: " + helpLine());
    }

    private static String helpLine() {
        return "color <COLOR> | totem <LETTER> | draw <ID> | end | state | quit";
    }
}