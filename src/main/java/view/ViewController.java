package view;

import network.client.core.LocalGameState;
import shared.dto.LobbyDto;
import shared.dto.PlayerDto;
import shared.dto.event.EndGameScoringDto;

import java.util.List;

/**
 * Interface for all view controllers.
 * Implement only the methods relevant to a given screen;
 * the defaults are intentional no-ops.
 */
public interface ViewController {

    /**
     * Called by the {@link network.client.core.ClientStateListenerGui} when a new game state snapshot is received from the server.
     * Each controller decides how to handle the update based on the current game phase and its own state.
     * @param state the game state given by the server
     */
    default void update(LocalGameState state) {}

    /**
     * Called by the {@link SceneRouter} and overridden by the {@link WaitingViewController}.
     * Shows the current lobby state in the waiting view.
     * @param lobby last lobby DTO state
     */
    default void setLobby(LobbyDto lobby) {}

    /**
     * Called by {@link network.client.core.ClientStateListenerGui} when a lobby update is received.
     * It's ovverridden by the {@link LobbyViewController} to show the list of lobbies.
     * @param lobbies list of lobbies DTO
     */
    default void showLobbies(List<LobbyDto> lobbies) {}

    /**
     * Called by {@link network.client.core.ClientStateListenerGui} when an update for a specific lobby is received.
     * Overridden by {@link LobbyViewController} to navigate to the waiting view when joining,
     * and by {@link WaitingViewController} to update with current lobby details (for example players count)
     *
     * @param lobby the updated state of the lobby
     */
    default void showLobbyState(LobbyDto lobby) {}

    /**
     * Called when the game ends prematurely (for example due to disconnections).
     * Displays the winner(s) without detailed end-game scoring breakdown.
     * Overridden by {@link WinnerViewController} to populate the end screen with basic player and winner information.
     *
     * @param players the list of players in the game
     * @param winners the list of winner usernames
     */
    default void showWinners(List<PlayerDto> players, List<String> winners) {}

    /**
     * Called when the game ends normally after all rounds are completed.
     * Displays the winner(s) alongside a detailed breakdown of the final points.
     * By default, it delegates to {@link #showWinners(List, List)} if detailed scoring is not supported.
     * Overridden by {@link WinnerViewController} to show full categorized score details.
     *
     * @param players the list of players in the game
     * @param winners the list of winner usernames
     * @param scoring the detailed scoring breakdown for all players
     */
    default void showWinners(List<PlayerDto> players, List<String> winners, EndGameScoringDto scoring) {
        showWinners(players, winners);
    }
}
