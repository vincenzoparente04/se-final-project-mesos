package client;

import shared.dto.CardDto;
import shared.dto.GameStateDto;
import shared.dto.OfferTileDto;
import shared.dto.PlayerDto;
import shared.dto.TurnOrderSlotDto;

import java.util.Collections;
import java.util.List;

/**
 * Client-side view of the game state.
 * Populated by deserialising {@link GameStateDto} snapshots received from the server.
 * <p>
 * The client never imports model domain classes (except enums) —
 * all game data arrives pre-serialised from the server.
 */
public class LocalGameState {

    private String phase;
    private String currentPlayerName;
    private int currentRound;
    private String currentEra;
    private List<PlayerDto> players = Collections.emptyList();
    private List<OfferTileDto> offerTiles = Collections.emptyList();
    private List<TurnOrderSlotDto> turnOrderSlots = Collections.emptyList();
    private List<CardDto> topRowTribe = Collections.emptyList();
    private List<CardDto> bottomRowTribe = Collections.emptyList();
    private List<CardDto> topRowBuilding = Collections.emptyList();
    private List<CardDto> bottomRowBuilding = Collections.emptyList();
    private List<String> winners = Collections.emptyList();

    public void update(GameStateDto dto) {
        this.phase             = dto.phase;
        this.currentPlayerName = dto.currentPlayerName;
        this.currentRound      = dto.currentRound;
        this.currentEra        = dto.currentEra;
        this.players           = dto.players           != null ? dto.players           : Collections.emptyList();
        this.offerTiles        = dto.offerTiles        != null ? dto.offerTiles        : Collections.emptyList();
        this.turnOrderSlots    = dto.turnOrderSlots    != null ? dto.turnOrderSlots    : Collections.emptyList();
        this.topRowTribe       = dto.topRowTribe       != null ? dto.topRowTribe       : Collections.emptyList();
        this.bottomRowTribe    = dto.bottomRowTribe    != null ? dto.bottomRowTribe    : Collections.emptyList();
        this.topRowBuilding    = dto.topRowBuilding    != null ? dto.topRowBuilding    : Collections.emptyList();
        this.bottomRowBuilding = dto.bottomRowBuilding != null ? dto.bottomRowBuilding : Collections.emptyList();
        this.winners           = dto.winners           != null ? dto.winners           : Collections.emptyList();
    }

    public String getPhase()             { return phase; }
    public String getCurrentPlayerName() { return currentPlayerName; }
    public int getCurrentRound()         { return currentRound; }
    public String getCurrentEra()        { return currentEra; }
    public List<PlayerDto> getPlayers()  { return players; }
    public List<OfferTileDto> getOfferTiles()       { return offerTiles; }
    public List<TurnOrderSlotDto> getTurnOrderSlots(){ return turnOrderSlots; }
    public List<CardDto> getTopRowTribe()            { return topRowTribe; }
    public List<CardDto> getBottomRowTribe()         { return bottomRowTribe; }
    public List<CardDto> getTopRowBuilding()         { return topRowBuilding; }
    public List<CardDto> getBottomRowBuilding()      { return bottomRowBuilding; }
    public List<String> getWinners()                 { return winners; }

    public boolean isGameOver() {
        return "END_OF_GAME".equals(phase);
    }
}
