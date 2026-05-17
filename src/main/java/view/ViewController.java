package view;

import network.client.LocalGameState;
import shared.dto.LobbyDto;
import shared.dto.PlayerDto;

import java.util.List;

/**
 * Interface for all view controllers.
 * Implement only the methods relevant to a given screen;
 * the defaults are intentional no-ops.
 */
public interface ViewController {

    default void update(LocalGameState state) {}

    default void setLobby(LobbyDto lobby) {}

    default void showLobbies(List<LobbyDto> lobbies) {}

    default void showLobbyState(LobbyDto lobby) {}

    default void showWinners(List<PlayerDto> players, List<String> winners) {}
}
