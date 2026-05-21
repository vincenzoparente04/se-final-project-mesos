package shared.dto.event;

import java.io.Serializable;

/**
 * Breakdown of the five end-game scoring sources for a single player.
 * The {@code details} string is server-formatted ready to display.
 */
public class PlayerScoringDeltaDto implements Serializable {
    public final String playerName;
    public final int prestigeBefore;
    public final int prestigeAfter;
    public final int buildersPoints;
    public final int artistsPoints;
    public final int inventorsPoints;
    public final int buildingPrintedPoints;
    public final int endGameBuildingEffectsPoints;
    public final String details;

    public PlayerScoringDeltaDto(String playerName,
                                 int prestigeBefore, int prestigeAfter,
                                 int buildersPoints, int artistsPoints,
                                 int inventorsPoints, int buildingPrintedPoints,
                                 int endGameBuildingEffectsPoints,
                                 String details) {
        this.playerName = playerName;
        this.prestigeBefore = prestigeBefore;
        this.prestigeAfter = prestigeAfter;
        this.buildersPoints = buildersPoints;
        this.artistsPoints = artistsPoints;
        this.inventorsPoints = inventorsPoints;
        this.buildingPrintedPoints = buildingPrintedPoints;
        this.endGameBuildingEffectsPoints = endGameBuildingEffectsPoints;
        this.details = details;
    }
}
