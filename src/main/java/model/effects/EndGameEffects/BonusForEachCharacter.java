package model.effects.EndGameEffects;

import model.player.Player;
import model.player.Tribe;

import java.util.List;
import java.util.function.ToIntFunction;

public class BonusForEachCharacter extends EndGameEffect {
    private final int multiplier;
    private final ToIntFunction<Tribe> correctGetter;

    /**
     * @implNote The constructor receives from the Building Factory the prestige points for each character and the reference
     * to the correct getter to count the correct character cards.
     * @param multiplier
     * @param correctGetter
     */
    public BonusForEachCharacter(int multiplier, ToIntFunction<Tribe> correctGetter) {
        this.multiplier = multiplier;
        this.correctGetter = correctGetter;
    }

    /**
     * @implNote uses the getter passed through the constructor to count the specified characters and add prestige points
     * for each character.
     * @param player
     */
    @Override
    public void applyEffect(Player player) {
        player.addPrestigePoints(correctGetter.applyAsInt(player.getTribe()) * multiplier);
    }

    /*
    Nella CardFactory quando viene creato l'effetto e passato alla buildingCard ci sarà un qualcosa di questo tipo in cui
    si passa il getter corretto

    // "3 PP per ogni Hunter"
    BuildingCard hunterBuilding = new BuildingCard(
    foodCost, era, printedPP,
    new PerCharacterTypeScoring(3, Tribe::getHunterCount)
    );

    // "2 PP per ogni Artist"
    BuildingCard artistBuilding = new BuildingCard(
    foodCost, era, printedPP,
    new PerCharacterTypeScoring(2, Tribe::getArtistCount)
    );

    // "4 PP per ogni Inventor"
    BuildingCard inventorBuilding = new BuildingCard(
    foodCost, era, printedPP,
    new PerCharacterTypeScoring(4, Tribe::getInventorCount)
    );
     */
}
