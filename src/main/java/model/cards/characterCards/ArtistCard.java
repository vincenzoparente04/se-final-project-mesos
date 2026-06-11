package model.cards.characterCards;

import model.enums.Era;
import model.player.Player;

/**
 * Represents a concrete Character card of type Artist within the tribe deck configuration.
 * <p>
 * Artists serve as specialized tribe members that interact directly with event-driven modifiers
 * (e.g., yielding food bonuses during Cave Paintings events via {@link model.cards.buildingCards.buildingEffects.onEventEffects.ArtistBonusForCavePaintings})
 * or economic relief conditions (such as sustenance discounts). Instances are parsed and built
 * by the {@link model.factories.TribeCardFactory}.
 * </p>
 */
public class ArtistCard extends CharacterCard {

    /**
     * Constructs a new concrete {@code ArtistCard} instance.
     *
     * @param id the unique sequential identifier assigned by the factory layer
     * @param era the chronological {@link Era} this card belongs to
     * @param playerCount the minimum player threshold required to inject this card into the active deck
     * @param imagePath the resource path for this card's front graphical asset
     * @param backImagePath the resource path for the standard tribe deck back asset
     */
    public ArtistCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    /**
     * <p>
     * Enrolls this instance into the target player's demographic inventory by invoking
     * {@link model.player.Tribe#addArtist(ArtistCard)}. This mutation dynamically updates the
     * artist population metrics, subsequently satisfying prerequisites for reactive building hooks,
     * event phase evaluations, and end-game scoring matrices.
     * </p>
     *
     * @param player the target {@link Player} whose tribe demographic state will include this card
     */
    @Override
    public void registerToTribe(Player player) {
        player.getTribe().addArtist(this);
    }
}