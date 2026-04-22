package shared.dto;

import java.io.Serializable;

public class CardDto implements Serializable {
    public final int id;
    public final String type;         // "CHARACTER", "EVENT", or "BUILDING"
    public final String era;          // Era.name()
    public final int foodCost;        // 0 for tribe cards
    public final int endGamePoints;   // 0 for tribe cards

    public CardDto(int id, String type, String era, int foodCost, int endGamePoints) {
        this.id = id;
        this.type = type;
        this.era = era;
        this.foodCost = foodCost;
        this.endGamePoints = endGamePoints;
    }
}
