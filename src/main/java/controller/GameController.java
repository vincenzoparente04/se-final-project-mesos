package controller;

import model.GameModel;
import model.board.OfferTile;
import model.cards.Card;
import model.player.Player;

import java.util.List;


public class GameController {
    private final GameModel gameModel;

    public GameController(GameModel gameModel) {
        this.gameModel = gameModel;
    }

    //functions to be called by client
    public void startGame(List<Player> players) {} //da capire come viene creata la partita. Come il client ci interagisce

    public void placeTotem(Player player, OfferTile offerTile) {
        //delega gameModel.placeModel()
    }
    public void drawCard(Player player, Card card) {
        //delega a gameModel.drawCard()
    }
}
