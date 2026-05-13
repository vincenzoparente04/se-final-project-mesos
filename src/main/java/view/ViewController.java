package view;


import network.client.LocalGameState;
import shared.dto.LobbyDto;
import shared.dto.PlayerDto;

import java.util.List;

public abstract class ViewController {

    void update(LocalGameState state) {}

    void bind(SceneRouter scene) {}

    void setLobby(LobbyDto lobby) {}

    public void showLobbies(List<LobbyDto> lobbies) {}

    public void showLobbyState(LobbyDto lobby) {}

    void showWinners(List<PlayerDto> players, List<String> winners) {}


}
