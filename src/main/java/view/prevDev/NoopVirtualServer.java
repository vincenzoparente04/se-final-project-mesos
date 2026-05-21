package view.prevDev;

import network.client.core.ClientStateListener;
import network.client.core.LocalGameState;
import network.client.core.VirtualServer;

/** Stub VirtualServer for UI previews — all commands are silently ignored. */
public class NoopVirtualServer implements VirtualServer {
    @Override public boolean tryRegisterName(String n, LocalGameState s, ClientStateListener l) { return true; }
    @Override public void start() {}
    @Override public void sendChooseColor(String color) {}
    @Override public void sendPlaceTotem(char tile) {}
    @Override public void sendDrawCard(int cardId) {}
    @Override public void sendEndTurn() {}
    @Override public void sendCreateLobby(int maxPlayers) {}
    @Override public void sendJoinLobby(String lobbyId) {}
    @Override public void sendListLobbies() {}
    @Override public void sendLeaveCommand() {}
    @Override public void close() {}
}
