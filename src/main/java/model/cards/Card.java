package model.cards;

import model.GameModel;
import model.enums.Era;
import model.player.Player;
import model.rowsManager.CardVisitor;

public abstract class Card {
    private final int id;
    private final Era era;           // ERA_I, ERA_II, ERA_III
    private final int playerCount;   // minimum number of players required to have this card in the game (some cards are only used in games with 3, 4 or 5 players)
    private final String imagePath;
    private final String backImagePath;

    public Card(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        this.id = id;
        this.era = era;
        this.playerCount = playerCount;
        this.imagePath = imagePath;
        this.backImagePath = backImagePath;
    }

    public int getId() {return id;}
    public Era getEra() {
        return era;
    }
    public int getMinPlayerCount() {
        return playerCount;
    }
    public String getImagePath() {
        return imagePath;
    }
    public String getBackImagePath() {
        return backImagePath;
    }

    /**
     * @implSpec  This method is called when a player acquires a card, add the card to
     * the player's tribe (specifically in the correct list of its type) and apply any effects it has.
     */
    public abstract void registerToTribe(Player player);

    public abstract void accept(CardVisitor visitor);
}
