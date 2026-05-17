package network.client.core.cli;

import network.client.core.ClientStateListener;
import network.client.core.LocalGameState;
import network.client.core.cli.view.GameStateRenderer;
import shared.dto.*;

import java.util.List;

public class ClientStateListenerCli implements ClientStateListener {
    private final String localPlayerName;
    private final GameStateRenderer renderer;

    public ClientStateListenerCli(String localPlayerName, GameStateRenderer renderer) {
        this.localPlayerName = localPlayerName;
        this.renderer = renderer;
    }
    @Override
    public synchronized void onGameStateUpdated(LocalGameState state) {
        GameStateDto dto = state.snapshot();

        renderer.render(dto, localPlayerName);
        System.out.print(contextualPrompt(state));
    }

    @Override
    public synchronized void onWaiting(String raw) {
        // raw = "current:expected"
        String[] parts = raw.split(":");
        String line = parts.length == 2
                ? "Players in lobby: " + parts[0] + "/" + parts[1]
                : raw;
        System.out.println("\n[WAITING] " + line);
        System.out.print("> ");
    }

    @Override
    public synchronized void onLobbyList(List<LobbyDto> lobbies) {
        System.out.println("\n[LOBBIES]");
        if (lobbies.isEmpty()) {
            System.out.println("  No open lobbies. Use 'create <maxPlayers>' to create one.");
        } else {
            for (LobbyDto l : lobbies) {
                System.out.printf("  %-36s  %-20s  %d/%d players%n",
                        l.id(), l.name(), l.currentPlayers(), l.maxPlayers());
            }
        }
        System.out.print("> ");
    }

    @Override
    public synchronized void onError(String message) {
        System.out.println("\n[ERROR] " + message);
        System.out.print("> ");
    }

    @Override
    public synchronized void onLobbyState(LobbyDto lobby) {
        System.out.printf("\n[LOBBY] %s — %d/%d players%n",
                lobby.name(), lobby.currentPlayers(), lobby.maxPlayers());
        System.out.print("> ");
    }

    @Override
    public synchronized void onGameStarting() {
        System.out.println("\n[GAME] All players joined — game starting!");
        System.out.print("> ");
    }

    @Override
    public synchronized void onGameOver(List<String> winners) {
        System.out.println("\n" + "═".repeat(48));
        if (winners.isEmpty()) {
            System.out.println("  GAME OVER — no winners.");
        } else {
            System.out.println("  GAME OVER — Winner(s): " + String.join(", ", winners));
        }
        System.out.println("═".repeat(48));
    }

    @Override
    public synchronized void onDisconnected() {
        System.out.println("\n[DISCONNECTED] Connection to server lost.");
    }

    /**
     * Forza il rendering dello stato e la stampa del prompt contestuale.
     * Utile quando l'utente richiede esplicitamente un refresh manuale della UI.
     */
    public void forceRefresh(LocalGameState state) {
        GameStateDto dto = state.snapshot();

        renderer.render(dto, localPlayerName);
        System.out.print(contextualPrompt(state));
    }

    public void printAllTribes(LocalGameState state) {
        renderer.renderAllTribes(state.snapshot(), localPlayerName);
    }


    //HEPLERS

    private static String helpForPhase(String phase) {
        if (phase == null) return "lobbies | create <n> | join <id>";
        return switch (phase) {
            case "COLOR_CHOOSING_PHASE" -> "color <RED|BLUE|GREEN|YELLOW|WHITE>";
            case "PLACEMENT"            -> "totem <LETTER>  (place your totem on a free offer tile)";
            case "ACTION"              -> "draw <cardId>   (pick from the visible rows above)";
            case "TURN_END"             -> "end             (confirm end of your turn)";
            default                     -> "state | quit";
        };
    }

    private String contextualPrompt(LocalGameState state) {
        GameStateDto dto = state.snapshot();
        if (dto == null) return "> ";

        boolean isMyTurn = localPlayerName.equals(dto.currentPlayerName);
        String label = isMyTurn ? "YOUR TURN — " + helpForPhase(dto.phase) : dto.phase;

        return "[" + label + "]\n> ";
    }
}
