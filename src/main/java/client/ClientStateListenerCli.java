package client;

import shared.dto.CardDto;
import shared.dto.GameStateDto;
import shared.dto.OfferTileDto;
import shared.dto.PlayerDto;

import java.util.List;

public class ClientStateListenerCli implements ClientStateListener{

    @Override
    public synchronized void onGameStateUpdated(LocalGameState state) {
        System.out.println();
        printState(state);
        System.out.print("> ");
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
    public synchronized void onError(String message) {
        System.out.println("\n[ERROR] " + message);
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



// ─────────────────────────────────────────────────────────
// State printer — reads from LocalGameState (AtomicReference)
// ─────────────────────────────────────────────────────────

    public static void printState(LocalGameState state) {
        GameStateDto dto = state.snapshot();
        if (dto == null) {
            System.out.println("[STATE] No state received yet.");
            return;
        }

        System.out.println("┌─ STATE ─────────────────────────────────────");
        System.out.printf("│ Phase: %-16s Round: %d   Era: %s%n",
                dto.phase, dto.currentRound,
                dto.currentEra != null ? dto.currentEra : "—");
        System.out.println("│ Current player: " +
                (dto.currentPlayerName != null ? dto.currentPlayerName : "—"));

        if (dto.players != null && !dto.players.isEmpty()) {
            System.out.println("├─ Players ───────────────────────────────────");
            for (PlayerDto p : dto.players) {
                System.out.printf("│  %-12s  food=%-3d  prestige=%-3d  color=%-6s  location=%s%n",
                        p.name, p.food, p.prestigePoints,
                        p.color != null ? p.color : "—",
                        p.totemLocation != null ? p.totemLocation : "—");
            }
        }

        if (dto.offerTiles != null && !dto.offerTiles.isEmpty()) {
            System.out.println("├─ Offer tiles ───────────────────────────────");
            for (OfferTileDto t : dto.offerTiles) {
                String occupant = t.occupantName != null ? "[" + t.occupantName + "]" : "[free]";
                String draws = "DRAW_CARDS".equals(t.actionType)
                        ? String.format(" top=%d/%s bot=%d/%s",
                        t.topRowUsed, t.topRowLimit,
                        t.bottomRowUsed, t.bottomRowLimit)
                        : "";
                System.out.printf("│  %c  %-12s %-10s%s%n",
                        t.letter, t.actionType, occupant, draws);
            }
        }

        if (dto.topRowTribe != null && !dto.topRowTribe.isEmpty()) {
            System.out.println("├─ Cards (top tribe) ─────────────────────────");
            printCards(dto.topRowTribe);
        }
        if (dto.bottomRowTribe != null && !dto.bottomRowTribe.isEmpty()) {
            System.out.println("├─ Cards (bottom tribe) ──────────────────────");
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
        System.out.println("└─────────────────────────────────────────────");
    }

    public static void printCards(List<CardDto> cards) {
        for (CardDto c : cards) {
            System.out.printf("│  id=%-4d  %-12s  era=%-5s  food=%-2d  pts=%d%n",
                    c.id, c.type, c.era, c.foodCost, c.endGamePoints);
        }
    }

}
