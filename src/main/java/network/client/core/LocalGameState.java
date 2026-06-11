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

    /** @return the current game phase, or {@code null} if no state received yet */
    public String getPhase() {
        GameStateDto s = snapshot();
        return s != null ? s.phase : null;
    }

    /** @return the name of the player whose turn it is, or {@code null} if none yet */
    public String getCurrentPlayerName() {
        GameStateDto s = snapshot();
        return s != null ? s.currentPlayerName : null;
    }

    /** @return the current round number, or {@code 0} if no state received yet */
    public int getCurrentRound() {
        GameStateDto s = snapshot();
        return s != null ? s.currentRound : 0;
    }

    /** @return the current era, or {@code null} if no state received yet */
    public String getCurrentEra() {
        GameStateDto s = snapshot();
        return s != null ? s.currentEra : null;
    }

    /** @return the players, or an empty list if unavailable (never {@code null}) */
    public List<PlayerDto> getPlayers() {
        GameStateDto s = snapshot();
        return s != null && s.players != null ? s.players : Collections.emptyList();
    }

    /** @return the offer-track tiles, or an empty list if unavailable (never {@code null}) */
    public List<OfferTileDto> getOfferTiles() {
        GameStateDto s = snapshot();
        return s != null && s.offerTiles != null ? s.offerTiles : Collections.emptyList();
    }

    /** @return the next-round turn-order slots, or an empty list if unavailable (never {@code null}) */
    public List<TurnOrderSlotDto> getTurnOrderSlots() {
        GameStateDto s = snapshot();
        return s != null && s.turnOrderSlots != null ? s.turnOrderSlots : Collections.emptyList();
    }

    /** @return the top-row character cards, or an empty list if unavailable (never {@code null}) */
    public List<CardDto> getTopRowTribe() {
        GameStateDto s = snapshot();
        return s != null && s.topRowTribe != null ? s.topRowTribe : Collections.emptyList();
    }

    /** @return the bottom-row character cards, or an empty list if unavailable (never {@code null}) */
    public List<CardDto> getBottomRowTribe() {
        GameStateDto s = snapshot();
        return s != null && s.bottomRowTribe != null ? s.bottomRowTribe : Collections.emptyList();
    }

    /** @return the top-row building cards, or an empty list if unavailable (never {@code null}) */
    public List<CardDto> getTopRowBuilding() {
        GameStateDto s = snapshot();
        return s != null && s.topRowBuilding != null ? s.topRowBuilding : Collections.emptyList();
    }

    /** @return the bottom-row building cards, or an empty list if unavailable (never {@code null}) */
    public List<CardDto> getBottomRowBuilding() {
        GameStateDto s = snapshot();
        return s != null && s.bottomRowBuilding != null ? s.bottomRowBuilding : Collections.emptyList();
    }

    /** @return the winners, or an empty list if not decided yet (never {@code null}) */
    public List<String> getWinners() {
        GameStateDto s = snapshot();
        return s != null && s.winners != null ? s.winners : Collections.emptyList();
    }

    /** @return {@code true} if the game has reached the {@code END_OF_GAME} phase */
    public boolean isGameOver() {
        return "END_OF_GAME".equals(getPhase());
    }
}
