package network.client;

import shared.dto.*;

import java.util.List;

public class ClientStateListenerCli implements ClientStateListener{
    private final String localPlayerName;

    public ClientStateListenerCli(String localPlayerName) {
        this.localPlayerName = localPlayerName;
    }

    @Override
    public synchronized void onGameStateUpdated(LocalGameState state) {
        System.out.println();
        printState(state);
        printCurrentPlayerTribe(state);        // mostra tribù di chi ha appena agito
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

// ─── State printer ───────────────────────────────────────────────────

    public void printState(LocalGameState state) {
        GameStateDto dto = state.snapshot();
        if (dto == null) { System.out.println("[STATE] No state received yet."); return; }

        printHeader(dto);
        printPlayers(dto);
        printOfferTrack(dto);
        printCardRows(dto);
        printMyTribe(dto);
        System.out.println("└─────────────────────────────────────────────");
    }

    private void printHeader(GameStateDto dto) {
        System.out.println("┌─ STATE ─────────────────────────────────────");
        System.out.printf("│ Phase: %-20s Round: %d   Era: %s%n",
                friendlyPhase(dto.phase), dto.currentRound,
                dto.currentEra != null ? dto.currentEra : "—");

        String cp = dto.currentPlayerName != null ? dto.currentPlayerName : "—";
        boolean isMyTurn = cp.equals(localPlayerName);
        System.out.println("│ Current player: " + cp + (isMyTurn ? "  ◄ YOUR TURN" : ""));
    }

    private void printPlayers(GameStateDto dto) {
        if (dto.players == null || dto.players.isEmpty()) return;
        System.out.println("├─ Players ───────────────────────────────────");
        for (PlayerDto p : dto.players) {
            String marker = localPlayerName.equals(p.name) ? "►" : " ";
            System.out.printf("│ %s %-12s  food=%-3d  prestige=%-3d  color=%-6s  loc=%s%n",
                    marker, p.name, p.food, p.prestigePoints,
                    p.color != null ? p.color : "—",
                    p.totemLocation != null ? p.totemLocation : "—");
        }
    }

    private static void printOfferTrack(GameStateDto dto) {
        if (dto.offerTiles == null || dto.offerTiles.isEmpty()) return;
        System.out.println("├─ Offer tiles ───────────────────────────────");
        for (OfferTileDto t : dto.offerTiles) {
            String occupant = t.occupantName != null ? "[" + t.occupantName + "]" : "[free]";
            String draws = "DRAW_CARDS".equals(t.actionType)
                    ? String.format(" top=%d/%s bot=%d/%s",
                    t.topRowUsed, t.topRowLimit, t.bottomRowUsed, t.bottomRowLimit)
                    : "";
            System.out.printf("│  %c  %-12s %-10s%s%n", t.letter, t.actionType, occupant, draws);
        }
    }

    private static void printCardRows(GameStateDto dto) {
        if (dto.topRowTribe != null && !dto.topRowTribe.isEmpty()) {
            System.out.println("├─ Cards (top row) ─────────────────────────");
            printCards(dto.topRowTribe);
        }
        if (dto.bottomRowTribe != null && !dto.bottomRowTribe.isEmpty()) {
            System.out.println("├─ Cards (bottom row) ──────────────────────");
            printCards(dto.bottomRowTribe);
        }
        if (dto.topRowBuilding != null && !dto.topRowBuilding.isEmpty()) {
            System.out.println("├─ Cards (top building) ──────────────────────");
            printCards(dto.topRowBuilding);
        }
        if (dto.bottomRowBuilding != null && !dto.bottomRowBuilding.isEmpty()) {
            System.out.println("├─ Cards (bottom building) ───────────────────");
            printCards(dto.bottomRowBuilding);
        }
    }

    private void printMyTribe(GameStateDto dto) {
        if (dto.players == null) return;
        dto.players.stream()
                .filter(p -> localPlayerName.equals(p.name))
                .findFirst()
                .ifPresent(p -> {
                    if (p.tribe == null) return;
                    System.out.println("├─ Your Tribe ────────────────────────────────");
                    if (p.tribe.characterCards.isEmpty() && p.tribe.buildings.isEmpty()) {
                        System.out.println("│  (empty — no cards yet)");
                    } else {
                        if (!p.tribe.characterCards.isEmpty()) {
                            System.out.println("│  Characters:");
                            printCards(p.tribe.characterCards);
                        }
                        if (!p.tribe.buildings.isEmpty()) {
                            System.out.println("│  Buildings:");
                            printCards(p.tribe.buildings);
                        }
                    }
                });
    }

    private void printCurrentPlayerTribe(LocalGameState state) {
        GameStateDto dto = state.snapshot();
        if (dto == null || dto.players == null || dto.currentPlayerName == null) return;
        if (dto.currentPlayerName.equals(localPlayerName)) return;
        dto.players.stream()
                .filter(p -> dto.currentPlayerName.equals(p.name))
                .findFirst()
                .ifPresent(p -> {
                    if (p.tribe == null || p.tribe.characterCards.isEmpty()) return;
                    System.out.println("├─ " + p.name + "'s updated tribe ──────────────────");
                    printCards(p.tribe.characterCards);
                    if (!p.tribe.buildings.isEmpty()) printCards(p.tribe.buildings);
                    System.out.println("└─────────────────────────────────────────────");
                });

    }

    private static void printCards(List<CardDto> cards) {
        for (CardDto c : cards) {
            // Mostra food/pts solo per i building (gli unici che li hanno significativi)
            String costInfo = "BUILDING".equals(c.type)
                    ? String.format("  cost=%-2d  pts=%-2d", c.foodCost, c.endGamePoints)
                    : "";
            String detailInfo = (c.details != null && !c.details.isEmpty())
                    ? "  [" + c.details + "]"
                    : "";
            System.out.printf("│    id=%-4d  %-10s  era=%-8s%s%s%n",
                    c.id, c.type, c.era, costInfo, detailInfo);
        }
    }


    //HEPLERS

    private static String friendlyPhase(String phase) {
        if (phase == null) return "—";
        return switch (phase) {
            case "SETUP"                -> "Setup";
            case "COLOR_CHOOSING_PHASE" -> "Choosing Colors";
            case "PLACEMENT"            -> "Placing Totems";
            case "ACTION"               -> "Resolving Actions";
            case "PRE_END_OF_ROUND"     -> "Pre-End of Round";
            case "END_OF_ROUND"         -> "End of Round";
            case "END_OF_GAME"          -> "Game Over";
            default                     -> phase;
        };
    }

    private static String helpForPhase(String phase) {
        if (phase == null) return "lobbies | create <n> | join <id>";
        return switch (phase) {
            case "COLOR_CHOOSING_PHASE" -> "color <RED|BLUE|GREEN|YELLOW|WHITE>";
            case "PLACEMENT"            -> "totem <LETTER>  (place your totem on a free offer tile)";
            case "DRAWING"              -> "draw <cardId>   (pick from the visible rows above)";
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
