package model.cards;

import model.enums.CardType;
import model.enums.CharacterType;
import model.enums.EventType;
import model.player.Player;
import model.player.Tribe;

import java.util.List;

public class CavePaintingsEventCard implements EventCard{
    /**
     * @implNote il calcolo è dovuto a un pattern ricorrente trovato negli eventi di questo tipo
     * @param players
     */
    @Override
    void resolve(List<Player> players) {
        for(Player p : players){
            int artists = p.getTribe().countByType(CharacterType.ARTIST);
            // se non ho il numero minimo di artisti pago
            if(artists < this.getEra().ordinal()){
                p.removePrestigePoints(2);
            }
            // altrimenti guadagno pp in base a quanti artisti ho
            else{
                p.addPrestigePoints(this.getEra().ordinal() * artists);
            }
        }
    }

    @Override
    public CardType getCardType() {
        return null;
    }
}
