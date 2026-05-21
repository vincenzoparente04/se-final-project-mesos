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
     * @implNote For each player, add to their food the number of hunters they have and add prestige points
     * equal to the number of hunters multiplied by the era of the card (ERA_I = 1, ERA_II = 2, ERA_III = 3).
     * Calls if eventually there are buildings who affect the hunter event.
     * @param players
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
