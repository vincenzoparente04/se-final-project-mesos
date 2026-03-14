package model;

import model.board.OfferTile;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.player.Player;

public interface GamePhaseHandler {
    /** Called when the phase begins. Initializes phase-specific state. */
    void onEnter();

    /**
     * Handles the main action a player performs in this phase.
     * @param player the player performing the action
     * @param target phase-specific target (TotemColor, OfferTile, cardId, etc.)
     */

    void chooseColor(Player player, TotemColor color) throws IllegalStateException;

    public void placeTotem(Player player, OfferTile offerTile);
    public boolean canPlaceTotem(Player player, OfferTile tile) throws Exception;

    public void drawCard(int cardId);


    /**
     * Checks whether the player can perform the given action.
     * @param player the player attempting the action
     * @param target phase-specific target
     * @return true if the action is valid
     */
    boolean canAct(Player player, Object target);

    /** Returns the enum value identifying this phase. */
    GamePhase getPhase();

    /** Returns the player whose turn it currently is (null for automatic phases). */
    Player getCurrentPlayer();
}
