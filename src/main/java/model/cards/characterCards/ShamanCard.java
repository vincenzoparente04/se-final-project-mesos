package model.cards.characterCards;

import model.enums.Era;
import model.player.Player;

/**
 * Represents a concrete Character card of type Shaman within the tribe deck configuration.
 * <p>
 * Shamans act as mystical and strategic demographic units characterized by an internal
 * {@code starCount} metric. These stars are evaluated dynamically during specific event phases
 * (such as Shamanic Ritual event resolutions) and heavily interface with picking-time modifiers
 * (such as structural flags like {@code ShamanicImmunity} or {@code ShamanicDoublePrestige}
 * injected via {@link model.cards.buildingCards.buildingEffects.onPickingEffects.OnPickingEffects}).
 * </p>
 */
public class ShamanCard extends CharacterCard {

    /**
     * The internal numeric magnitude of shamanic stars carried by this specific character,
     * serving as a multiplier or threshold check for ritual events and advanced scoring.
     */
    private final int starCount;

    /**
     * Constructs a new concrete {@code ShamanCard} instance with a designated star count.
     *
     * @param id            the unique sequential identifier assigned by the factory layer
     * @param era           the chronological {@link Era} this card belongs to
     * @param playerCount   the minimum player threshold required to inject this card into the active deck
     * @param starCount     the absolute number of ritual stars associated with this shaman
     * @param imagePath     the resource path for this card's front graphical asset
     * @param backImagePath the resource path for the standard tribe deck back asset
     */
    public ShamanCard(int id, Era era, int playerCount, int starCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
        this.starCount = starCount;
    }

    /**
     * Retrieves the magnitude of shamanic stars bound to this card.
     *
     * @return the integer value of the star counter
     */
    public int getStarCount(){
        return starCount;
    }

    /*
     * <p>
     * Enrolls this instance into the target player's demographic inventory by invoking
     * {@link model.player.Tribe#addShaman(ShamanCard)}. This mutation updates the tribe's
     * aggregate star weight and population index, immediately affecting reactive event listeners
     * and scoring mechanisms.
     * </p>
     *
     * @param player the target {@link Player} whose tribe demographic state will include this card
     */
    @Override
    public void registerToTribe(Player player) {
        player.getTribe().addShaman(this);
    }
}