package shared.dto;

import java.io.Serializable;

public class CardDto implements Serializable {
    public final int id;
    public final String type;         // "CHARACTER", "EVENT", or "BUILDING"
    public final String era;          // Era.name()
    public final int foodCost;        // 0 for tribe cards
    public final int endGamePoints;   // 0 for tribe cards
    public final String ImagePath;
    public final String backImagePath;

    public CardDto(int id, String type, String era, int foodCost, int endGamePoints, String ImagePath, String backImagePath) {
        this.id = id;
        this.type = type;
        this.era = era;
        this.foodCost = foodCost;
        this.endGamePoints = endGamePoints;
        this.ImagePath = ImagePath;
        this.backImagePath = backImagePath;
    }
}
