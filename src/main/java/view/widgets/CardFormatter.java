package view.widgets;

import shared.dto.CardDto;

/**
 * Pure-static formatting helpers for card type labels and metadata strings.
 * Centralises the three duplicated copies that existed across
 * TribePopupController, BoardGameAreaController and CardZoomOverlay.
 */
public final class CardFormatter {

    private CardFormatter() {}

    /**
     * Returns the plural display name for a card sub-type,
     * used in tribe group headers ("Hunters ×3", etc.).
     */
    public static String prettyType(String type) {
        if (type == null) return "?";
        return switch (type) {
            case "HUNTER" -> "Hunters";
            case "BUILDER" -> "Builders";
            case "SHAMAN" -> "Shamans";
            case "ARTIST" -> "Artists";
            case "INVENTOR" -> "Inventors";
            case "GATHERER" -> "Gatherers";
            default -> type;
        };
    }

    /**
     * Returns the singular display name for a card,
     * used in the card-zoom overlay header.
     * Includes building and event types.
     */
    public static String formatType(CardDto c) {
        if (c == null || c.type == null) return "";
        return switch (c.type) {
            case "HUNTER" -> "Hunter";
            case "BUILDER" -> "Builder";
            case "SHAMAN" -> "Shaman";
            case "ARTIST" -> "Artist";
            case "INVENTOR" -> "Inventor";
            case "GATHERER" -> "Gatherer";
            case "EVENT" -> "Event";
            case "BUILDING" -> "Building";
            default -> c.type;
        };
    }

    /**
     * Compact metadata string for tribe popup cells.
     * Format: "details • cost X • +YPP"
     */
    public static String buildCardMeta(CardDto c) {
        StringBuilder sb = new StringBuilder();
        if (c.details != null && !c.details.isBlank()) sb.append(c.details);
        if (c.foodCost > 0) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append("cost ").append(c.foodCost);
        }
        if (c.endGamePoints > 0) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append("+").append(c.endGamePoints).append("PP");
        }
        return sb.toString();
    }

    /**
     * Full metadata string for the card-zoom overlay.
     * Format: "details   •   food cost: X   •   end-game: Y PP   •   Era Z"
     */
    public static String buildMetaText(CardDto c) {
        StringBuilder sb = new StringBuilder();
        if (c.details != null && !c.details.isBlank()) sb.append(c.details);
        if (c.foodCost > 0) {
            if (sb.length() > 0) sb.append("   •   ");
            sb.append("food cost: ").append(c.foodCost);
        }
        if (c.endGamePoints > 0) {
            if (sb.length() > 0) sb.append("   •   ");
            sb.append("end-game: ").append(c.endGamePoints).append(" PP");
        }
        if (c.era != null) {
            if (sb.length() > 0) sb.append("   •   ");
            sb.append("Era ").append(c.era);
        }
        return sb.toString();
    }
}
