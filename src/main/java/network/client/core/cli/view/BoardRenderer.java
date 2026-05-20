package network.client.core.cli.view;

import shared.dto.*;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;
import shared.dto.event.PlayerEventDeltaDto;
import shared.dto.event.PlayerScoringDeltaDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete CLI renderer. Separated from ClientStateListenerCli (SRP):
 * the listener handles network events, this class handles all display logic.
 * The GameStateRenderer abstraction will also allow a JavaFX implementation
 * to reuse the same interface without touching event handling code.
 */
public class BoardRenderer implements GameStateRenderer {

    // Offer tile column dimensions
    private static final int TILE_WIDTH  = 14;   // including borders
    private static final int TILE_INNER  = 10;   // TILE_WIDTH - 4
    private static final int TILE_HEIGHT = 7;

    // ─── GameStateRenderer ───────────────────────────────────────────────────

    @Override
    public void render(GameStateDto dto, String localPlayerName) {
        if (dto == null) { System.out.println("[STATE] No state received yet."); return; }
        clearScreen();
        printHeader(dto, localPlayerName);
        printTurnOrder(dto);
        printPlayers(dto, localPlayerName);
        printOfferTrack(dto);
        printBoardCards(dto);
        printMyTribe(dto, localPlayerName);
    }

    @Override
    public void renderAllTribes(GameStateDto dto, String localPlayerName) {
        if (dto == null || dto.players == null) {
            System.out.println("[TRIBES] Nessuno stato disponibile.");
            return;
        }
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║          TRIBÙ DI TUTTI I GIOCATORI                  ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        for (PlayerDto p : dto.players) {
            String marker = localPlayerName.equals(p.name) ? " ◄ (tu)" : "";
            System.out.printf("%n── %s%s ─── Cibo: %d | PP: %d%n",
                    p.name, marker, p.food, p.prestigePoints);
            if (p.tribe == null || (p.tribe.characterCards.isEmpty() && p.tribe.buildings.isEmpty())) {
                System.out.println("  (Nessuna carta)");
            } else {
                if (!p.tribe.characterCards.isEmpty()) {
                    System.out.println("  Personaggi:");
                    CardPrinter.printCardsHorizontally(p.tribe.characterCards);
                }
                if (!p.tribe.buildings.isEmpty()) {
                    System.out.println("  Edifici:");
                    CardPrinter.printCardsHorizontally(p.tribe.buildings);
                }
            }
        }
        System.out.println("──────────────────────────────────────────────────────");
    }

    // ─── Sections ────────────────────────────────────────────────────────────

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void printHeader(GameStateDto dto, String localPlayerName) {
        String cp      = dto.currentPlayerName != null ? dto.currentPlayerName : "—";
        boolean myTurn = cp.equals(localPlayerName);
        System.out.println("═".repeat(78));
        System.out.printf("  Fase: %-22s  Round: %-3d  Era: %s%n",
                friendlyPhase(dto.phase), dto.currentRound,
                dto.currentEra != null ? "ERA " + dto.currentEra : "—");
        System.out.printf("  Turno di: %s%s%n", cp, myTurn ? "  ◄  È IL TUO TURNO!" : "");
        System.out.println("═".repeat(78));
    }

    /**
     * Turn order is critical strategic info: whoever is first chooses
     * the best offer tile before others. Shown here so players can plan ahead.
     */
    private void printTurnOrder(GameStateDto dto) {
        if (dto.turnOrderSlots == null || dto.turnOrderSlots.isEmpty()) return;
        System.out.print("[ ORDINE TURNO (prossimo round) ]");
        for (TurnOrderSlotDto slot : dto.turnOrderSlots) {
            String name = slot.occupantName != null ? slot.occupantName : "—";
            System.out.printf("   %d°→%s", slot.position + 1, name);
        }
        System.out.println("\n");
    }

    /**
     * Each player row includes a one-line tribe summary.
     * This lets players track: shaman stars (for Shamanic Ritual outcome),
     * hunter count (for Hunt event food/PP), builder discount (for building costs).
     * Use the "tribes" command for full card details.
     */
    private void printPlayers(GameStateDto dto, String localPlayerName) {
        if (dto.players == null || dto.players.isEmpty()) return;
        System.out.println("[ GIOCATORI ]");
        for (PlayerDto p : dto.players) {
            String mark = localPlayerName.equals(p.name) ? "►" : " ";
            System.out.printf("%s %-12s  Colore: %-6s  Cibo: %-3d  PP: %-4d  Totem: %s%n",
                    mark, p.name,
                    p.color != null ? p.color : "—",
                    p.food, p.prestigePoints,
                    p.totemLocation != null ? p.totemLocation : "—");
            System.out.printf("  ↳ %s%n", formatTribeSummary(p));
        }
        System.out.println();
    }

    /**
     * Each tile is rendered as a TILE_HEIGHT-line column and printed side by side.
     *
     * Column layout:
     *   line 0: top border
     *   line 1: letter + action type (PESCA / CIBO)
     *   line 2: occupant (● taken, ○ free)
     *   line 3: blank spacer
     *   line 4: top-row draw status  (↑ usa/max ✓/✗/—)
     *   line 5: bottom-row draw status
     *   line 6: bottom border
     *
     * Status symbols: ✓ draws available, ✗ exhausted, — row not available on this tile.
     */
    private void printOfferTrack(GameStateDto dto) {
        if (dto.offerTiles == null || dto.offerTiles.isEmpty()) return;
        System.out.println("[ TRACCIATO OFFERTE ]");

        List<String[]> columns = new ArrayList<>();
        for (OfferTileDto t : dto.offerTiles) columns.add(buildTileColumn(t));

        for (int row = 0; row < TILE_HEIGHT; row++) {
            StringBuilder sb = new StringBuilder("  ");
            for (String[] col : columns) sb.append(col[row]).append("  ");
            System.out.println(sb);
        }
        System.out.println("  ↑/↓ = fila sup/inf   usa/max   ✓ disponibile  ✗ esaurita  — non disponibile");
        System.out.println();
    }

    private String[] buildTileColumn(OfferTileDto t) {
        String[] lines      = new String[TILE_HEIGHT];
        boolean  isDrawTile = "DRAW_CARDS".equals(t.actionType);
        String   title      = pad("[" + t.letter + "] " + (isDrawTile ? "PESCA" : "CIBO"), TILE_INNER);
        String   occupant   = (t.occupantName != null ? "● " : "○ ")
                + (t.occupantName != null ? t.occupantName : "(libero)");
        String   topStat    = drawStatus("↑", t.topRowUsed,    t.topRowLimit);
        String   botStat    = drawStatus("↓", t.bottomRowUsed, t.bottomRowLimit);

        lines[0] = "┌" + "─".repeat(TILE_WIDTH - 2) + "┐";
        lines[1] = "│ " + title                          + " │";
        lines[2] = "│ " + pad(truncate(occupant, TILE_INNER), TILE_INNER) + " │";
        lines[3] = "│" + " ".repeat(TILE_WIDTH - 2)      + "│";
        lines[4] = "│ " + pad(topStat,  TILE_INNER)       + " │";
        lines[5] = "│ " + pad(botStat,  TILE_INNER)       + " │";
        lines[6] = "└" + "─".repeat(TILE_WIDTH - 2) + "┘";
        return lines;
    }

    /**
     * Returns a draw-status label for one row of a tile.
     * max==null or max==0 → row not available on this tile.
     */
    private static String drawStatus(String arrow, Integer used, Integer max) {
        if (max == null || max == 0) return arrow + " —";
        int u   = used != null ? used : 0;
        String sym = u < max ? "✓" : "✗";
        return String.format("%s %d/%d %s", arrow, u, max, sym);
    }

    /**
     * Tribe cards and building cards are printed in separate sub-sections
     * with different headers. Since CardPrinter already uses different box styles
     * (┌─┐ vs ╔═╗), the visual distinction is reinforced at two levels.
     */
    private void printBoardCards(GameStateDto dto) {
        printRow("FILA SUPERIORE", dto.topRowTribe,    dto.topRowBuilding);
        printRow("FILA INFERIORE", dto.bottomRowTribe, dto.bottomRowBuilding);
        System.out.println("─".repeat(78));
    }

    private void printRow(String label, List<CardDto> tribe, List<CardDto> buildings) {
        System.out.println("[ " + label + " ]");
        boolean hasTribes    = tribe    != null && !tribe.isEmpty();
        boolean hasBuildings = buildings != null && !buildings.isEmpty();
        if (!hasTribes && !hasBuildings) {
            System.out.println("  (Nessuna carta)");
        } else {
            if (hasTribes) {
                System.out.println("  ─── Personaggi ───────────────────────────────────────");
                CardPrinter.printCardsHorizontally(tribe);
            }
            if (hasBuildings) {
                System.out.println("  ═══ Edifici ═══════════════════════════════════════════");
                CardPrinter.printCardsHorizontally(buildings);
            }
        }
        System.out.println();
    }

    private void printMyTribe(GameStateDto dto, String localPlayerName) {
        if (dto.players == null) return;
        dto.players.stream()
                .filter(p -> localPlayerName.equals(p.name))
                .findFirst()
                .ifPresent(p -> {
                    System.out.printf("[ LA TUA TRIBÙ — %s | Cibo: %d | PP: %d ]%n",
                            p.name, p.food, p.prestigePoints);
                    if (p.tribe == null || (p.tribe.characterCards.isEmpty() && p.tribe.buildings.isEmpty())) {
                        System.out.println("  (Nessuna carta)");
                        return;
                    }
                    if (!p.tribe.characterCards.isEmpty()) {
                        System.out.println("  ─── Personaggi ────────────────────────────────────");
                        CardPrinter.printCardsHorizontally(p.tribe.characterCards);
                    }
                    if (!p.tribe.buildings.isEmpty()) {
                        System.out.println("  ═══ Edifici ════════════════════════════════════════");
                        CardPrinter.printCardsHorizontally(p.tribe.buildings);
                    }
                });
    }

    // ─── Tribe summary ───────────────────────────────────────────────────────

    /**
     * Builds a compact one-line tribe summary for the players table.
     * Includes type-specific annotations:
     * - Shamans: total star count, relevant for Shamanic Ritual majority check
     * - Builders: total food discount, relevant when buying buildings
     * - Gatherers: show count, each gives -3 food during Sustenance
     */
    private static String formatTribeSummary(PlayerDto p) {
        if (p.tribe == null) return "—";
        List<CardDto> chars = p.tribe.characterCards;
        if (chars.isEmpty() && p.tribe.buildings.isEmpty()) return "(vuota)";

        List<String> parts = new ArrayList<>();
        if (count(chars, "HUNTER")   > 0) parts.add(count(chars, "HUNTER")   + " Cac");
        if (count(chars, "SHAMAN")   > 0) parts.add(count(chars, "SHAMAN")   + " Sci(" + countStars(chars) + "★)");
        if (count(chars, "BUILDER")  > 0) parts.add(count(chars, "BUILDER")  + " Cos(-" + totalBuilderDiscount(chars) + "f)");
        if (count(chars, "ARTIST")   > 0) parts.add(count(chars, "ARTIST")   + " Art");
        if (count(chars, "INVENTOR") > 0) parts.add(count(chars, "INVENTOR") + " Inv");
        if (count(chars, "GATHERER") > 0) parts.add(count(chars, "GATHERER") + " Rac");
        if (!p.tribe.buildings.isEmpty())  parts.add(p.tribe.buildings.size() + " Edi");
        return parts.isEmpty() ? "(vuota)" : String.join(" | ", parts);
    }

    private static long count(List<CardDto> cards, String type) {
        return cards.stream().filter(c -> type.equals(c.type)).count();
    }

    /** Counts total ★ across all shaman detail strings. */
    private static int countStars(List<CardDto> cards) {
        return (int) cards.stream()
                .filter(c -> "SHAMAN".equals(c.type) && c.details != null)
                .flatMapToInt(c -> c.details.chars())
                .filter(ch -> ch == '★')
                .count();
    }

    /**
     * Extracts total builder discount by parsing the leading "-N" from details
     * strings like "-2 food/bldg | +3 PP end".
     * Acceptable coupling: the format is fully controlled by GameStateDtoBuilder.
     */
    private static int totalBuilderDiscount(List<CardDto> cards) {
        return cards.stream()
                .filter(c -> "BUILDER".equals(c.type) && c.details != null)
                .mapToInt(c -> {
                    int idx = c.details.indexOf('-');
                    if (idx < 0) return 0;
                    int end = idx + 1;
                    while (end < c.details.length() && Character.isDigit(c.details.charAt(end))) end++;
                    try { return Integer.parseInt(c.details.substring(idx + 1, end)); }
                    catch (NumberFormatException e) { return 0; }
                })
                .sum();
    }

    // ─── String helpers ──────────────────────────────────────────────────────

    private static String friendlyPhase(String phase) {
        if (phase == null) return "—";
        return switch (phase) {
            case "SETUP"                -> "Setup";
            case "COLOR_CHOOSING_PHASE" -> "Scelta Colori";
            case "PLACEMENT"            -> "Posizionamento Totem";
            case "ACTION"               -> "Risoluzione Azioni";
            case "PRE_END_OF_ROUND"     -> "Pre-Fine Round";
            case "END_OF_ROUND"         -> "Fine Round";
            case "END_OF_GAME"          -> "Fine Partita";
            default                     -> phase;
        };
    }

    private static String pad(String s, int w) {
        if (s == null) s = "";
        return s.length() >= w ? s.substring(0, w) : s + " ".repeat(w - s.length());
    }

    private static String truncate(String s, int w) {
        if (s == null) return "";
        return s.length() <= w ? s : s.substring(0, w - 2) + "..";
    }

    // ─── Event resolution rendering ──────────────────────────────────────

    private static final int EVENT_BOX_WIDTH = 70;

    @Override
    public void renderEvent(EventResolutionDto resolution, String localPlayerName) {
        if (resolution == null) return;
        System.out.println();
        printBoxHeader(resolution.headline);
        for (PlayerEventDeltaDto d : resolution.deltas) {
            String marker = d.playerName.equals(localPlayerName) ? " ◄ (tu)" : "       ";
            System.out.printf("  %-12s%s  food %d → %-3d  prestige %d → %-4d  (%s)%n",
                    truncate(d.playerName, 12), marker,
                    d.foodBefore, d.foodAfter,
                    d.prestigeBefore, d.prestigeAfter,
                    d.details == null ? "" : d.details);
        }
        System.out.println();
    }

    @Override
    public void renderEndGameScoring(EndGameScoringDto scoring, List<String> winners, String localPlayerName) {
        if (scoring == null) return;
        System.out.println();
        printBoxHeader("END-GAME SCORING");
        // Column header
        System.out.printf("  %-12s %-7s   %4s %4s %4s %4s %4s   %4s → %4s%n",
                "Player", "", "Bld", "Art", "Inv", "BPP", "Eff", "PP", "PP");
        for (PlayerScoringDeltaDto d : scoring.deltas) {
            String marker = d.playerName.equals(localPlayerName) ? "◄ (tu)" : "      ";
            System.out.printf("  %-12s %-7s   %+4d %+4d %+4d %+4d %+4d   %4d → %4d%n",
                    truncate(d.playerName, 12), marker,
                    d.buildersPoints, d.artistsPoints, d.inventorsPoints,
                    d.buildingPrintedPoints, d.endGameBuildingEffectsPoints,
                    d.prestigeBefore, d.prestigeAfter);
        }
        // Winners line
        System.out.println();
        String winnerLine = (winners == null || winners.isEmpty())
                ? "GAME OVER — no winners"
                : "GAME OVER — Winner(s): " + String.join(", ", winners);
        System.out.println("  " + winnerLine);
        System.out.println();
    }

    private static void printBoxHeader(String title) {
        String t = title == null ? "" : title;
        int innerWidth = EVENT_BOX_WIDTH - 2;
        int pad = Math.max(0, innerWidth - t.length());
        int left = pad / 2;
        int right = pad - left;
        System.out.println("╔" + "═".repeat(innerWidth) + "╗");
        System.out.println("║" + " ".repeat(left) + t + " ".repeat(right) + "║");
        System.out.println("╚" + "═".repeat(innerWidth) + "╝");
    }
}