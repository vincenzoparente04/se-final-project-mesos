package model.cards.eventCards;

import model.buildingEffects.OnEventEffects.OnEventBuildingEffect;
import model.enums.CardType;
import model.enums.CharacterType;
import model.player.Player;

import java.util.List;

public class CavePaintingsEventCard extends EventCard {

    public CavePaintingsEventCard(int id, Era era, int playerCount) {
        super(id, era, playerCount);
    }

    /**
     * @implNote il calcolo è dovuto a un pattern ricorrente trovato negli eventi di questo tipo
     * @param players
     */
    @Override
    public void resolve(List<Player> players) {
        for(Player p : players){
            int artists = p.getTribe().;
            // se non ho il numero minimo di artisti pago
            if(artists < this.getEra().ordinal()){
                p.removePrestigePoints(2);
            }
            // altrimenti guadagno pp in base a quanti artisti ho
            else{
                p.addPrestigePoints(this.getEra().ordinal() * artists);
            }

            // building effects
            for (OnEventBuildingEffect effect : p.getTribe().getOnEventBuildingEffects()) {
                effect.applyOnCavePaintings(p);
            }
        }
    }
}
