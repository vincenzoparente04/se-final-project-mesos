package network.client.core;

import shared.dto.CardDto;
import shared.dto.GameStateDto;
import shared.dto.OfferTileDto;
import shared.dto.PlayerDto;
import shared.dto.TurnOrderSlotDto;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client-side view of the game state.
 * Populated by deserialising {@link GameStateDto} snapshots received from the server.
 * <p>
 * The internal {@link AtomicReference} guarantees that every getter always reads a consistent,
 * fully-written snapshot with no synchronisation overhead on reads.
 */
public class LocalGameState {

    private final AtomicReference<GameStateDto> latest = new AtomicReference<>();

    public void update(GameStateDto dto) {
        latest.set(dto);
    }

    /**
     * Returns the latest complete snapshot received from the server,
     * or {@code null} if no state has been received yet.
     */
    public GameStateDto snapshot() {
        return latest.get();
    }

    public String getPhase() {
        GameStateDto s = snapshot();
        return s != null ? s.phase : null;
    }

    public String getCurrentPlayerName() {
        GameStateDto s = snapshot();
        return s != null ? s.currentPlayerName : null;
    }

    public int getCurrentRound() {
        GameStateDto s = snapshot();
        return s != null ? s.currentRound : 0;
    }

    public String getCurrentEra() {
        GameStateDto s = snapshot();
        return s != null ? s.currentEra : null;
    }

    public List<PlayerDto> getPlayers() {
        GameStateDto s = snapshot();
        return s != null && s.players != null ? s.players : Collections.emptyList();
    }

    public List<OfferTileDto> getOfferTiles() {
        GameStateDto s = snapshot();
        return s != null && s.offerTiles != null ? s.offerTiles : Collections.emptyList();
    }

    public List<TurnOrderSlotDto> getTurnOrderSlots() {
        GameStateDto s = snapshot();
        return s != null && s.turnOrderSlots != null ? s.turnOrderSlots : Collections.emptyList();
    }

    public List<CardDto> getTopRowTribe() {
        GameStateDto s = snapshot();
        return s != null && s.topRowTribe != null ? s.topRowTribe : Collections.emptyList();
    }

    public List<CardDto> getBottomRowTribe() {
        GameStateDto s = snapshot();
        return s != null && s.bottomRowTribe != null ? s.bottomRowTribe : Collections.emptyList();
    }

    public List<CardDto> getTopRowBuilding() {
        GameStateDto s = snapshot();
        return s != null && s.topRowBuilding != null ? s.topRowBuilding : Collections.emptyList();
    }

    public List<CardDto> getBottomRowBuilding() {
        GameStateDto s = snapshot();
        return s != null && s.bottomRowBuilding != null ? s.bottomRowBuilding : Collections.emptyList();
    }

    public List<String> getWinners() {
        GameStateDto s = snapshot();
        return s != null && s.winners != null ? s.winners : Collections.emptyList();
    }

    public boolean isGameOver() {
        return "END_OF_GAME".equals(getPhase());
    }
}
