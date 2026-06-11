package model.cards.eventCards;

import model.rowsManager.CardVisitor;
import model.cards.buildingCards.buildingEffects.onEventEffects.OnEventBuildingEffect;
import model.enums.Era;
import model.enums.EventType;
import model.player.Player;
import shared.dto.event.EventResolutionDto;
import shared.dto.event.PlayerEventDeltaDto;

import java.util.ArrayList;
import java.util.List;

public class SustenanceEventCard extends EventCard {

    public SustenanceEventCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }


    /**
     * Each player must feed their entire tribe. The food requirement equals the
     * total character count minus gatherer discounts and any building-provided
     * reductions. Shortfalls are passed to
     * {@link model.player.Player#removeFoodWithPrestigePenalty(int, int)},
     * which drains food first and converts any remaining deficit to prestige
     * losses scaled by the era index.
     *
     * @param players the list of active players
     * @return an {@link EventResolutionDto} summarising the per-player outcome
     */
    @Override
    public EventResolutionDto resolve(List<Player> players) {
        int eraIndex = this.getEra().ordinal() + 1;
        List<PlayerEventDeltaDto> deltas = new ArrayList<>();

        for (Player p : players) {
            int foodBefore = p.getFood();
            int prestigeBefore = p.getPrestigePoints();
            int characterCount = p.getTribe().getTotalCharacterCount();
            int discount = p.getTribe().getTotalGatherersDiscount();

            for (OnEventBuildingEffect effect : p.getTribe().getOnEventBuildingEffects()) {
                discount += effect.applyOnSustenance(p);
            }

            int requirement = Math.max(0, characterCount - discount);
            if (requirement > 0) {
                p.removeFoodWithPrestigePenalty(requirement, eraIndex);
            }

            int foodAfter = p.getFood();
            int prestigeAfter = p.getPrestigePoints();
            String details = "%d chars, %d covered → %+d food, %+d prestige"
                    .formatted(characterCount, discount,
                            foodAfter - foodBefore, prestigeAfter - prestigeBefore);

            deltas.add(new PlayerEventDeltaDto(p.getName(),
                    foodBefore, foodAfter, prestigeBefore, prestigeAfter, details));
        }

        return new EventResolutionDto(
                EventType.SUSTENANCE.name(), this.getEra().name(), this.getId(),
                "Sustenance - " + this.getEra().name(),
                deltas);
    }

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}
