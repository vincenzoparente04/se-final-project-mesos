package network.client.core.cli.view;

import shared.dto.GameStateDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;

import java.util.List;

/**
 * Renders the game state to the console. Keeping the rendering behind this
 * interface lets {@code ClientStateListenerCli} delegate presentation instead of
 * owning it (which would violate the single-responsibility principle).
 */
public interface GameStateRenderer {

    /**
     * Renders the full board for the local player.
     *
     * @param state the current game-state snapshot ({@code null} renders nothing)
     * @param localPlayerName the local player's name, used to tailor the view
     */
    void render(GameStateDto state, String localPlayerName);

    /**
     * Renders every tribe row in full.
     *
     * @param dto the current game-state snapshot
     * @param localPlayerName the local player's name
     */
    void renderAllTribes(GameStateDto dto, String localPlayerName);

    /**
     * Renders the per-player breakdown of a single resolved event card.
     *
     * @param resolution the resolved-event payload
     * @param localPlayerName the local player's name
     */
    void renderEvent(EventResolutionDto resolution, String localPlayerName);

    /**
     * Renders the end-game scoring breakdown (five entries per player) plus the
     * winners line.
     *
     * @param scoring the end-game scoring breakdown
     * @param winners the winning player names
     * @param localPlayerName the local player's name
     */
    void renderEndGameScoring(EndGameScoringDto scoring, List<String> winners, String localPlayerName);
}
