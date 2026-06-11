package shared.dto;

import java.io.Serializable;

/**
 * Serializable view of an offer (action) tile: its letter, action type, the
 * occupant (if any) and, for draw tiles, the per-row draw limits and how many
 * draws have already been used this turn. The {@code Integer} fields are
 * {@code null} for {@code TAKE_FOOD} tiles; {@code occupantName} is {@code null}
 * when the tile is free.
 */
public class OfferTileDto implements Serializable {
    public final char letter;
    public final String actionType;       // "DRAW_CARDS" or "TAKE_FOOD"
    public final String occupantName;     // null if the tile is free
    public final Integer topRowLimit;     // max draws from top row; null for TAKE_FOOD
    public final Integer bottomRowLimit;  // max draws from bottom row; null for TAKE_FOOD
    public final Integer topRowUsed;      // draws already used from top row this turn; null for TAKE_FOOD
    public final Integer bottomRowUsed;   // draws already used from bottom row this turn; null for TAKE_FOOD

    public OfferTileDto(char letter, String actionType, String occupantName,
                        Integer topRowLimit, Integer bottomRowLimit,
                        Integer topRowUsed, Integer bottomRowUsed) {
        this.letter = letter;
        this.actionType = actionType;
        this.occupantName = occupantName;
        this.topRowLimit = topRowLimit;
        this.bottomRowLimit = bottomRowLimit;
        this.topRowUsed = topRowUsed;
        this.bottomRowUsed = bottomRowUsed;
    }
}
