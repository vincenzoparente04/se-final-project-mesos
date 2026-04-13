package shared.dto;

import java.util.List;

public class GameStateDto {
    public final String phase;              // GamePhase.name()
    public final String currentPlayerName;  // null when no active player
    public final int currentRound;
    public final String currentEra;         // Era.name(), null before setup
    public final List<PlayerDto> players;
    public final List<OfferTileDto> offerTiles;
    public final List<TurnOrderSlotDto> turnOrderSlots;
    public final List<CardDto> topRowTribe;
    public final List<CardDto> bottomRowTribe;
    public final List<CardDto> topRowBuilding;
    public final List<CardDto> bottomRowBuilding;
    public final List<String> winners;      // null until END_OF_GAME

    public GameStateDto(
            String phase,
            String currentPlayerName,
            int currentRound,
            String currentEra,
            List<PlayerDto> players,
            List<OfferTileDto> offerTiles,
            List<TurnOrderSlotDto> turnOrderSlots,
            List<CardDto> topRowTribe,
            List<CardDto> bottomRowTribe,
            List<CardDto> topRowBuilding,
            List<CardDto> bottomRowBuilding,
            List<String> winners) {
        this.phase = phase;
        this.currentPlayerName = currentPlayerName;
        this.currentRound = currentRound;
        this.currentEra = currentEra;
        this.players = players;
        this.offerTiles = offerTiles;
        this.turnOrderSlots = turnOrderSlots;
        this.topRowTribe = topRowTribe;
        this.bottomRowTribe = bottomRowTribe;
        this.topRowBuilding = topRowBuilding;
        this.bottomRowBuilding = bottomRowBuilding;
        this.winners = winners;
    }
}
