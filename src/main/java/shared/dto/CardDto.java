package shared.dto;

import java.io.Serializable;

/**
 * Serializable view of a single card — a character/event/building card or a
 * tribe card. Carries the data the client needs to render the card (costs,
 * end-game points, special-effect {@code details}, front/back image paths);
 * enums such as the type and era are passed as their {@code name()}. Cost and
 * point fields are {@code 0} for tribe cards.
 */
public class CardDto implements Serializable {
    public final int id;
    public final String type;         // "CHARACTER", "EVENT", or "BUILDING"
    public final String era;          // Era.name()
    public final int foodCost;        // 0 for tribe cards
    public final int endGamePoints;   // 0 for tribe cards
    public final String details;      // to signal special effects caused by certain character chards
    public final String ImagePath;
    public final String backImagePath;

    public CardDto(int id, String type, String era, int foodCost, int endGamePoints, String details, String ImagePath, String backImagePath) {
        this.id = id;
        this.type = type;
        this.era = era;
        this.foodCost = foodCost;
        this.endGamePoints = endGamePoints;
        this.details       = details;
        this.ImagePath = ImagePath;
        this.backImagePath = backImagePath;
    }
}
