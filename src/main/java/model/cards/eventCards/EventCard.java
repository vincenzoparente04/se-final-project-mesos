package model.cards.eventCards;

import model.rowsManager.CardVisitor;
import model.cards.TribeCard;
import model.enums.Era;
import model.player.Player;
import shared.dto.event.EventResolutionDto;

import java.util.List;

public abstract class EventCard extends TribeCard {
    public EventCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    @Override
    public void registerToTribe(Player player) { return; }

    /**
     * Applies the event side-effects on the given players and returns a
     * structured snapshot of what happened (per-player food/prestige before
     * and after, plus a server-formatted {@code details} string explaining
     * the calculation). The {@code EndOfRoundPhase} broadcasts one
     * {@code EventResolvedMessage} per returned DTO.
     */
    public abstract EventResolutionDto resolve(List<Player> players);

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}
