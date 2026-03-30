package model.cards.eventCards;

import model.buildingEffects.OnEventEffects.OnEventBuildingEffect;
import model.enums.Era;
import model.player.Player;

import java.util.List;

public class CavePaintingsEventCard extends EventCard {

    public CavePaintingsEventCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    /**
     * @implNote For each player, if they have less artists than the era of the card, they lose 2 prestige points.
     * Otherwise, they gain prestige points equal to the number of artists multiplied by the era of the card.
     * Calls if eventually there are buildings who affect the cave paintings event.
     * @param players
     */
    @Override
    public void resolve(List<Player> players) {
        for(Player p : players){
            int artists = p.getTribe().getArtistCount();
            if(artists < this.getEra().ordinal() + 1){
                p.removePrestigePoints(2);
            }
            else{
                p.addPrestigePoints((this.getEra().ordinal() + 1 ) * artists);
            }

            // building effects
            for (OnEventBuildingEffect effect : p.getTribe().getOnEventBuildingEffects()) {
                effect.applyOnCavePaintings(p);
            }
        }
    }
}
