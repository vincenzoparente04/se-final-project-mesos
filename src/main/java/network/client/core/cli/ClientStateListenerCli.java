package network.client.core.cli;

import database.ScoreRecord;
import network.client.core.ClientStateListener;
import network.client.core.LocalGameState;
import network.client.core.cli.view.GameStateRenderer;
import shared.dto.*;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;

import java.util.List;

public class ClientStateListenerCli implements ClientStateListener {
    private final String localPlayerName;
    private final GameStateRenderer renderer;
    private List<ScoreRecord> scoreRecord = null;
    private int rankPosition;
    private int points;

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
    public synchronized void onEventResolved(EventResolutionDto resolution) {
        renderer.renderEvent(resolution, localPlayerName);
        System.out.print("> ");
    }

    @Override
    public synchronized void onGameOver(List<String> winners, EndGameScoringDto scoring) {
        if (scoring != null) {
            renderer.renderEndGameScoring(scoring, winners, localPlayerName);

            if (this.scoreRecord != null) {
                System.out.println("\nTop 20 Leaderboard : " + scoreRecord.getFirst().playerCount() + " players matches");
                System.out.println("  " + "─".repeat(61));
                System.out.printf("  %-4s|%-20s|%-7s|%-20s%n", "Pos", "Player", "Score", "Date");
                System.out.println("  " + "─".repeat(61));

                int i = 1;
                for (ScoreRecord record : scoreRecord) {
                    System.out.printf("%4d | %-20s | %7d | %-20s%n", i, record.nickname(), record.score(), record.date());
                    i++;
                }
                System.out.println("  " + "─".repeat(61));
                System.out.print("  Your Result: Ranked " + rankPosition + "° with " +  points + " points\n");
                System.out.println("state | quit | leave");
                System.out.print("> ");
            }
        } else {
            // forfait (suspension timeout): solo annuncio dei winners
            System.out.println("\n" + "═".repeat(48));
            if (winners == null || winners.isEmpty()) {
                System.out.println("  GAME OVER — no winners.");
            } else {
                System.out.println("  GAME OVER — Winner(s): " + String.join(", ", winners));
            }
            System.out.println("═".repeat(48));
        }
    }

    @Override
    public void onLeaderboardUpdate(List<ScoreRecord> leaderboard, int rankPosition, int points) {
        this.scoreRecord = leaderboard;
        this.rankPosition = rankPosition;
        this.points = points;
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
            case "COLOR_CHOOSING_PHASE"  -> "color <RED|BLUE|GREEN|YELLOW|WHITE>";
            case "PLACEMENT"             -> "totem <LETTER>  (place your totem on a free offer tile)";
            case "ACTION"                -> "draw <cardId>   (pick from the visible rows above)";
            case "TURN_END"              -> "end             (confirm end of your turn)";
            case "PRE_END_OF_ROUND"      -> "state | quit | end";
            default                      -> "state | quit";
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
