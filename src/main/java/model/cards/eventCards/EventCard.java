package model.cards.eventCards;

import model.GameModel;
import model.rowsManager.CardVisitor;
import model.cards.TribeCard;
import model.enums.Era;
import model.player.Player;

import java.util.List;

public abstract class EventCard extends TribeCard {
    public EventCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    @Override
    public void registerToTribe(Player player) { return; }

    public abstract void resolve(List<Player> players);

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}