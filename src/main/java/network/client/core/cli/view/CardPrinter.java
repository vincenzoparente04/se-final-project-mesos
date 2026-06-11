package network.client.core.cli.view;

import shared.dto.CardDto;
import java.util.ArrayList;
import java.util.List;

/**
 * Prints cards as ASCII boxes ("GUI-like" style) on the console. Encapsulates the
 * matrix-of-strings layout logic so callers only pass {@link CardDto}s.
 */
public class CardPrinter {
    /** Total card width in characters, borders included. */
    private static final int CARD_WIDTH    = 22;
    /** Usable text width inside the borders ({@code CARD_WIDTH - 4}). */
    private static final int INNER_WIDTH   = 18;
    /** Card height in lines. */
    private static final int CARD_HEIGHT   = 8;
    /** Cards printed side by side before wrapping to a new row. */
    private static final int CARDS_PER_ROW = 5;

    /**
     * Prints the given cards as ASCII boxes, wrapping every {@value #CARDS_PER_ROW}
     * cards onto a new row.
     *
     * @param cards the cards to print (a {@code null} or empty list prints a placeholder)
     */
    public static void printCardsHorizontally(List<CardDto> cards) {
        if (cards == null || cards.isEmpty()) {
            System.out.println("  (Nessuna carta presente)");
            return;
        }
        for (int start = 0; start < cards.size(); start += CARDS_PER_ROW) {
            printBatch(cards.subList(start, Math.min(start + CARDS_PER_ROW, cards.size())));
            if (start + CARDS_PER_ROW < cards.size()) System.out.println();
        }
    }

    /**
     * Prints one row of up to {@value #CARDS_PER_ROW} cards, line by line.
     *
     * @param batch the cards in this row
     */
    private static void printBatch(List<CardDto> batch) {
        List<String[]> rendered = new ArrayList<>();
        for (CardDto c : batch) rendered.add(buildCardAscii(c));
        for (int row = 0; row < CARD_HEIGHT; row++) {
            StringBuilder sb = new StringBuilder();
            for (String[] ascii : rendered) sb.append(ascii[row]).append("  ");
            System.out.println(sb);
        }
    }

    /**
     * Builds the {@value #CARD_HEIGHT}-line ASCII art for one card (double border for
     * buildings, single border otherwise).
     *
     * @param card the card to render
     * @return the card's lines, top to bottom
     */
    private static String[] buildCardAscii(CardDto card) {
        String[] lines   = new String[CARD_HEIGHT];
        boolean  isBldg  = "BUILDING".equals(card.type);

        String header  = pad("#" + card.id, 9) + pad("Era " + nvl(card.era, "?"), INNER_WIDTH - 9);
        String typeStr = center(nvl(card.type, "?"), INNER_WIDTH);
        List<String> det = parseDetails(card);

        if (isBldg) {
            String costPp = "Cost:" + card.foodCost + "   PP:" + card.endGamePoints;
            lines[0] = "╔" + "═".repeat(CARD_WIDTH - 2) + "╗";
            lines[1] = "║ " + header                        + " ║";
            lines[2] = "║ " + typeStr                       + " ║";
            lines[3] = "╠" + "═".repeat(CARD_WIDTH - 2) + "╣";
            lines[4] = "║ " + pad(costPp,       INNER_WIDTH) + " ║";
            lines[5] = "║ " + pad(get(det, 0),  INNER_WIDTH) + " ║";
            lines[6] = "║ " + pad(get(det, 1),  INNER_WIDTH) + " ║";
            lines[7] = "╚" + "═".repeat(CARD_WIDTH - 2) + "╝";
        } else {
            lines[0] = "┌" + "─".repeat(CARD_WIDTH - 2) + "┐";
            lines[1] = "│ " + header                        + " │";
            lines[2] = "│ " + typeStr                       + " │";
            lines[3] = "├" + "─".repeat(CARD_WIDTH - 2) + "┤";
            lines[4] = "│ " + pad(get(det, 0),  INNER_WIDTH) + " │";
            lines[5] = "│ " + pad(get(det, 1),  INNER_WIDTH) + " │";
            lines[6] = "│ " + pad(get(det, 2),  INNER_WIDTH) + " │";
            lines[7] = "└" + "─".repeat(CARD_WIDTH - 2) + "┘";
        }
        return lines;
    }

    /**
     * Splits a card's {@code details} on {@code '|'} and word-wraps each part to
     * {@value #INNER_WIDTH} columns.
     *
     * @param card the card whose details to lay out
     * @return the wrapped detail lines
     */
    private static List<String> parseDetails(CardDto card) {
        List<String> result = new ArrayList<>();
        if (card.details == null || card.details.isEmpty()) return result;
        for (String part : card.details.split("\\|")) {
            String s = part.trim();
            while (s.length() > INNER_WIDTH) {
                int cut = s.lastIndexOf(' ', INNER_WIDTH);
                if (cut <= 0) cut = INNER_WIDTH;
                result.add(s.substring(0, cut));
                s = s.substring(cut).trim();
            }
            if (!s.isEmpty()) result.add(s);
        }
        return result;
    }


    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** @return {@code list.get(i)}, or {@code ""} if {@code i} is out of range */
    private static String get(List<String> list, int i) {
        return i < list.size() ? list.get(i) : "";
    }
    /** @return {@code s} padded with spaces (or truncated) to width {@code w} */
    private static String pad(String s, int w) {
        if (s == null) s = "";
        return s.length() >= w ? s.substring(0, w) : s + " ".repeat(w - s.length());
    }
    /** @return {@code s} centered (or truncated) within width {@code w} */
    private static String center(String s, int w) {
        if (s == null) s = "";
        if (s.length() >= w) return s.substring(0, w);
        int l = (w - s.length()) / 2;
        return " ".repeat(l) + s + " ".repeat(w - s.length() - l);
    }
    /** @return {@code s} if non-null and non-empty, otherwise {@code def} */
    private static String nvl(String s, String def) {
        return (s != null && !s.isEmpty()) ? s : def;
    }

}
