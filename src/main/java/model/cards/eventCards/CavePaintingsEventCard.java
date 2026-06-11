package model.cards.eventCards;

import model.cards.buildingCards.buildingEffects.onEventEffects.OnEventBuildingEffect;
import model.enums.Era;
import model.enums.EventType;
import model.player.Player;
import shared.dto.event.EventResolutionDto;
import shared.dto.event.PlayerEventDeltaDto;

import java.util.ArrayList;
import java.util.List;

public class CavePaintingsEventCard extends EventCard {

    public CavePaintingsEventCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    /**
     * For each player, if their artist count is below the era index they lose
     * 2 prestige points; otherwise they gain prestige equal to artists times the
     * era index. Then applies any active building effects via
     * {@code applyOnCavePaintings}.
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
            int artists = p.getTribe().getArtistCount();

            if (artists < eraIndex) {
                p.removePrestigePoints(2);
            } else {
                p.addPrestigePoints(eraIndex * artists);
            }

            // building effects
            for (OnEventBuildingEffect effect : p.getTribe().getOnEventBuildingEffects()) {
                effect.applyOnCavePaintings(p);
            }

            int foodAfter = p.getFood();
            int prestigeAfter = p.getPrestigePoints();
            String details = "%d artists (req %d) → %+d prestige"
                    .formatted(artists, eraIndex, prestigeAfter - prestigeBefore);

            deltas.add(new PlayerEventDeltaDto(p.getName(), foodBefore, foodAfter, prestigeBefore, prestigeAfter, details));
        }

        return new EventResolutionDto(EventType.CAVE_PAINTINGS.name(), this.getEra().name(), this.getId(),
                "Cave paintings - " + this.getEra().name(),
                deltas);
    }
}
