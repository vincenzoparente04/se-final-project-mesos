package model.cards.eventCards;

import model.rowsManager.CardVisitor;
import model.cards.buildingCards.buildingEffects.onEventEffects.OnEventBuildingEffect;
import model.enums.Era;
import model.enums.EventType;
import model.player.Player;
import shared.dto.event.EventResolutionDto;
import shared.dto.event.PlayerEventDeltaDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the global concrete event execution card for the Sustenance phase.
 * <p>
 * This event class implements the core economic maintenance check of the game engine,
 * requiring each player to allocate resources to feed their entire accumulated population.
 * The algorithm aggregates structural discounts from gatherer characters and queries active
 * building effects to mitigate costs. Any unbacked population deficit triggers a compounding
 * resource debt evaluation that converts into prestige penalties scaled by the active {@link Era}.
 * </p>
 */
public class SustenanceEventCard extends EventCard {

    /**
     * Constructs a new concrete {@code SustenanceEventCard} instance.
     *
     * @param id the unique sequential identifier assigned by the factory layer
     * @param era the chronological {@link Era} this card is bound to
     * @param playerCount the minimum player threshold required to inject this card into play
     * @param imagePath the resource path for this card's front graphical asset
     * @param backImagePath the resource path for the standard event deck back asset
     */
    public SustenanceEventCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    /**
     * <p>
     * <b>Algorithmic Resolution Pipeline:</b>
     * <ol>
     * <li>Computes the chronological scaling index: {@code eraIndex = era.ordinal() + 1}.</li>
     * <li>Iterates through each active game participant to evaluate demographic maintenance prerequisites.</li>
     * <li><b>Baseline Discount Compilation:</b> Retrieves the initial resource discount yielded inherently
     * by gatherer cards inside the tribe via {@link model.player.Tribe#getTotalGatherersDiscount()}.</li>
     * <li><b>Building Hook Interception:</b> Dispatches a polling loop to all registered
     * {@link OnEventBuildingEffect} instances via {@link OnEventBuildingEffect#applyOnSustenance(Player)}
     * to accumulate additional structural or conditional food cost reductions.</li>
     * <li><b>Requirement Balancing:</b> Calculates the net required food as {@code Math.max(0, population - discount)}.</li>
     * <li><b>Penalty Resolution:</b> If the net requirement strictly exceeds zero, delegates the subtraction
     * to {@link Player#removeFoodWithPrestigePenalty(int, int)}, which exhausts the current food inventory
     * before applying a converted prestige point loss scaled by {@code eraIndex}.</li>
     * <li><b>State Serialization:</b> Packages individual asset deltas and details metrics into client-bound
     * {@link PlayerEventDeltaDto} data wrappers.</li>
     * </ol>
     * </p>
     *
     * @param players the active list of {@link Player} instances participating in the game
     * @return a fully populated, serialized network transport object {@link EventResolutionDto} summarizing the phase outcomes
     */
    @Override
    public EventResolutionDto resolve(List<Player> players) {
        int eraIndex = this.getEra().ordinal() + 1;
        List<PlayerEventDeltaDto> deltas = new ArrayList<>();

        for (Player p : players) {
            int foodBefore = p.getFood();
            int prestigeBefore = p.getPrestigePoints();
            int characterCount = p.getTribe().getTotalCharacterCount();
            int discount = p.getTribe().getTotalGatherersDiscount();

            // Accumulates additional sustenance mitigation from active structures
            for (OnEventBuildingEffect effect : p.getTribe().getOnEventBuildingEffects()) {
                discount += effect.applyOnSustenance(p);
            }

            int requirement = Math.max(0, characterCount - discount);
            if (requirement > 0) {
                p.removeFoodWithPrestigePenalty(requirement, eraIndex);
            }

            int foodAfter = p.getFood();
            int prestigeAfter = p.getPrestigePoints();
            String details = "%d chars, %d covered → %+d food, %+d prestige"
                    .formatted(characterCount, discount,
                            foodAfter - foodBefore, prestigeAfter - prestigeBefore);

            deltas.add(new PlayerEventDeltaDto(p.getName(),
                    foodBefore, foodAfter, prestigeBefore, prestigeAfter, details));
        }

        return new EventResolutionDto(
                EventType.SUSTENANCE.name(), this.getEra().name(), this.getId(),
                "Sustenance - " + this.getEra().name(),
                deltas);
    }

    /**
     * <p>
     * Routes the execution path directly to the sustenance event card specialization of the visitor.
     * This ensures type-safe double-dispatch processing by row management or game rule evaluation
     * sub-systems without necessitating manual casting operations.
     * </p>
     *
     * @param visitor the {@link CardVisitor} performing structural evaluations or operations on this card
     */
    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}