package network.client.view;

import shared.dto.CardDto;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe per stampare le carte come se fossero in "stile gui"
 * Incapsula la logica di manipolazione delle stringhe a matrice.
 */
public class CardPrinter {
    private static final int CARD_WIDTH    = 22;   // including borders
    private static final int INNER_WIDTH   = 18;   // CARD_WIDTH - 4
    private static final int CARD_HEIGHT   = 8;
    private static final int CARDS_PER_ROW = 5;

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

    private static void printBatch(List<CardDto> batch) {
        List<String[]> rendered = new ArrayList<>();
        for (CardDto c : batch) rendered.add(buildCardAscii(c));
        for (int row = 0; row < CARD_HEIGHT; row++) {
            StringBuilder sb = new StringBuilder();
            for (String[] ascii : rendered) sb.append(ascii[row]).append("  ");
            System.out.println(sb);
        }
    }

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

    private static String get(List<String> list, int i) {
        return i < list.size() ? list.get(i) : "";
    }
    private static String pad(String s, int w) {
        if (s == null) s = "";
        return s.length() >= w ? s.substring(0, w) : s + " ".repeat(w - s.length());
    }
    private static String center(String s, int w) {
        if (s == null) s = "";
        if (s.length() >= w) return s.substring(0, w);
        int l = (w - s.length()) / 2;
        return " ".repeat(l) + s + " ".repeat(w - s.length() - l);
    }
    private static String nvl(String s, String def) {
        return (s != null && !s.isEmpty()) ? s : def;
    }

}
