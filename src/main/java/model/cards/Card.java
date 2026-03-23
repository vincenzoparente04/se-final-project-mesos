package model.cards;

import model.GameModel;
import model.enums.Era;
import model.player.Player;

public abstract class Card implements Drawable {
    private final int id;
    private final Era era;           // ERA_I, ERA_II, ERA_III
    private final int playerCount;   // minimum number of players required to have this card in the game (some cards are only used in games with 3, 4 or 5 players)

    public Card(int id, Era era, int playerCount) {
        this.id = id;
        this.era = era;
        this.playerCount = playerCount;
    }

    public int getId() { return id; }
    public Era getEra() { return era; }
    public int getMinPlayerCount() { return playerCount; }

    public abstract void registerToTribe(Player player);
}
