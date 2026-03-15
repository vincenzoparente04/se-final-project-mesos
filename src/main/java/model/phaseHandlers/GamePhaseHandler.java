package model.phaseHandlers;

import model.board.OfferTile;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.player.Player;

public interface GamePhaseHandler {
    /** Called when the phase begins. Initializes phase-specific state. */
    void onEnter();

    void chooseColor(Player player, TotemColor object) throws IllegalStateException;

    public void placeTotem(Player player, OfferTile offerTile);

    public void drawCard(int cardId);


    /** Returns the enum value identifying this phase. */
    GamePhase getPhase();

    /** Returns the player whose turn it currently is (null for automatic phases). */
    Player getCurrentPlayer();
}
