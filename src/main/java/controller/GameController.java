package controller;

import model.GameModel;
import model.board.OfferTile;
import model.cards.Card;
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
    public void placeTotem(Player player, OfferTile offerTile) {
        gameModel.placeTotem(player, offerTile);
    }

    /**
     * @implNote Is called by the client and forwards the color choice request to the gameModel.
     * The client sends their chosen totem color during the setup phase.
     * @param player The player making the color choice
     * @param color The totem color chosen by the player
     */
    public void chooseColor(Player player, TotemColor color) {
        try {
            gameModel.chooseColor(player, color);
        } catch (IllegalStateException e) {
            // Handle invalid color choice - notify the client about the error
            System.err.println("Invalid color choice: " + e.getMessage());
            throw e; // Re-throw so the client can handle it
        }
    }

    public void drawCard(Player player, Card card) {
        //delega a gameModel.drawCard()
    }
}
