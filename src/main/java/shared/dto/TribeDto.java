package shared.dto;

import java.util.List;

/**
 * DTO snapshot of a player's tribe (acquired cards).
 * Character cards and buildings are serialised separately.
 */
public class TribeDto {
    public final List<CardDto> characterCards;
    public final List<CardDto> buildings;

    public TribeDto(List<CardDto> characterCards, List<CardDto> buildings) {
        this.characterCards = characterCards;
        this.buildings = buildings;
    }
}
