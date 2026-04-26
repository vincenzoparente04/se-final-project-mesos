package client;

/**
 * Translates GUI events into commands sent to the server via {@link VirtualServer}.
 * The View calls these methods; this class knows nothing about the View's
 * internal structure or the underlying transport protocol.
 */
public class ClientController {

    private final VirtualServer serverProxy;

    public ClientController(VirtualServer serverProxy) {
        this.serverProxy = serverProxy;
    }

    public void onColorChosen(String color) {
        serverProxy.sendChooseColor(color);
    }

    public void onTotemPlaced(char tile) {
        serverProxy.sendPlaceTotem(tile);
    }

    public void onCardDrawn(int cardId) {
        serverProxy.sendDrawCard(cardId);
    }

    public void onTurnEnded() {
        serverProxy.sendEndTurn();
    }
}
