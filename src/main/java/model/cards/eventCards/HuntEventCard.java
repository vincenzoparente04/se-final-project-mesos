package model.cards.eventCards;

import model.cards.buildingCards.buildingEffects.onEventEffects.OnEventBuildingEffect;
import model.enums.Era;
import model.enums.EventType;
import model.player.Player;
import shared.dto.event.EventResolutionDto;
import shared.dto.event.PlayerEventDeltaDto;

import java.util.ArrayList;
import java.util.List;

public class HuntEventCard extends EventCard {

    public HuntEventCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    /**
     * For each player, adds food and prestige equal to their hunter count scaled
     * by the era index (1 for ERA_I, 2 for ERA_II, 3 for ERA_III). Then applies
     * any active building effects via {@code applyOnHunt}.
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
            int hunters = p.getTribe().getHunterCount();

            p.addFood(hunters);
            p.addPrestigePoints(hunters * eraIndex);

            // building effects
            for (OnEventBuildingEffect effect : p.getTribe().getOnEventBuildingEffects()) {
                effect.applyOnHunt(p);
            }

            int foodAfter = p.getFood();
            int prestigeAfter = p.getPrestigePoints();
            String details = "%d hunters → %+d food, %+d prestige"
                    .formatted(hunters, foodAfter - foodBefore, prestigeAfter - prestigeBefore);

            deltas.add(new PlayerEventDeltaDto(p.getName(), foodBefore, foodAfter, prestigeBefore, prestigeAfter, details));
        }

        return new EventResolutionDto(EventType.HUNT.name(), this.getEra().name(), this.getId(),
                "Hunt event - " + this.getEra().name(), deltas);
    }
}
