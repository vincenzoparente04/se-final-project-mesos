package model.buildingEffects.OnCharacterAcquiredEffects;

import model.cards.characterCards.InventorCard;
import model.enums.InventionIcon;
import model.player.Player;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


// TODO ricontrollatelo che non capisco tutte le funzioni usate
public class InventorsPairBonus extends OnAcquireBuildingEffect {
    private Set<InventionIcon> alreadyPaired;

    /**
     * @implNote Override registerSelf() to save the already paired icons in player's tribe when this building is acquired
     * @param player
     */
    @Override
    public void registerSelf(Player player) {
        // fotografa le coppie al momento dell'acquisto
        this.alreadyPaired = EnumSet.noneOf(InventionIcon.class);
        for (Map.Entry<InventionIcon, List<InventorCard>> entry
                : player.getTribe().getInventorsByIcon().entrySet()) {
            if (entry.getValue().size() >= 2) {
                alreadyPaired.add(entry.getKey());
            }
        }
        player.getTribe().registerOnAcquireEffect(this);
    }

    /**
     * @implNote After each draw this method checks if a new pair has been completed, comparing the map of the previous
     * round to player's tribe
     * @param player
     */
    @Override
    public void applyEffect(Player player) {
        for (Map.Entry<InventionIcon, List<InventorCard>> entry
                : player.getTribe().getInventorsByIcon().entrySet()) {
            if (entry.getValue().size() >= 2 && alreadyPaired.add(entry.getKey())) {
                player.addFood(3);
                return;
            }
        }
    }
}
