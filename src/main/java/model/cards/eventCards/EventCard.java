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
     * Applies the event side-effects on all players and returns a structured
     * snapshot of what happened (per-player food/prestige deltas plus a
     * server-formatted details string). {@code EndOfRoundPhase} broadcasts one
     * {@code EventResolvedMessage} per returned DTO.
     *
     * @param players the list of active players
     * @return an {@link EventResolutionDto} summarising the per-player outcome
     */
    public abstract EventResolutionDto resolve(List<Player> players);

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}
