package network.client;

public class ClientController {

    private final VirtualServer virtualServer;

    public ClientController(VirtualServer virtualServer) {
        this.virtualServer = virtualServer;
    }

    public void onColorChosen(String color) {
        virtualServer.sendChooseColor(color);
    }

    public void onTotemPlaced(char tile) {
        virtualServer.sendPlaceTotem(tile);
    }

    public void onCardDrawn(int cardId) {
        virtualServer.sendDrawCard(cardId);
    }

    public void onTurnEnded() {
        virtualServer.sendEndTurn();
    }

    public void onCreateLobby(int maxPlayers) {
        virtualServer.sendCreateLobby(maxPlayers);
    }

    public void onJoinLobby(String lobbyId) {
        virtualServer.sendJoinLobby(lobbyId);
    }

    public void onListLobbies() {
        virtualServer.sendListLobbies();
    }
}
