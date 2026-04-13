package client;

/**
 * Translates GUI events into commands sent to the server via {@link VirtualServer}.
 * The View calls these methods; this class knows nothing about the View's
 * internal structure.
 */
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
}
