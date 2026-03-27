package controller;

import model.GameModel;
import model.board.OfferTile;
import model.enums.TotemColor;
import model.player.Player;

import java.util.List;


public class GameController {
    private final GameModel gameModel;

    public GameController(GameModel gameModel) {
        this.gameModel = gameModel;
    }

    //functions to be called by client
    public void startGame(List<String> players) {
        gameModel.startGame(players);
    } //da capire come viene creata la partita. Come il client ci interagisce

    /**
     * @implNote Is called by the clients and forwards the request to the gameModel.
     * @param player
     * @param offerTile
     */
    public void placeTotem(Player player, char tileId) throws Exception {
        gameModel.placeTotem(player, tileId);
    }

    /**
     * @implNote Is called by the client and forwards the color choice request to the gameModel.
     * The client sends their chosen totem color during the setup phase.
     * @param player The player making the color choice
     * @param color The totem color chosen by the player
     */
    public void chooseColor(Player player, TotemColor color) {
        gameModel.chooseColor(player, color);
    }

    /**
     * @implNote Is called by the client and forwards the card picked by the player the gameModel.
     * The client sends his chosen card color during the action phase.
     * @param player The player making the card choice
     * @param cardId is selected card's ID.
     */
    public void drawCard(Player player, int cardId) throws Exception {
       gameModel.drawCard(cardId);
    }
}
