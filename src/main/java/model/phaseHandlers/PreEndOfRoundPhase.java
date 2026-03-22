package model.phaseHandlers;

import model.GameModel;
import model.effects.PreEndOfRoundEffects.PreEndOfRoundEffect;
import model.enums.GamePhase;
import model.phaseHandlers.EndOfRoundPhase;
import model.phaseHandlers.GamePhaseHandler;
import model.player.Player;

public class PreEndOfRoundPhase extends GamePhaseHandler {

    private Player waitingForInput;

    public PreEndOfRoundPhase(GameModel model) {
        super(model);
    }

    @Override
    public void onEnter() {
        // prima risolvi tutti gli effetti automatici
        for (Player p : model.getPlayers()) {
            for (PreEndOfRoundEffect effect : p.getPreEndOfRoundEffects()) {
                if (!effect.needsPlayerInput()) {
                    effect.applyEffect(p);
                }
            }
        }

        // poi cerca se qualcuno ha un effetto interattivo
        waitingForInput = findPlayerNeedingInput();

        if (waitingForInput == null) {
            model.setPhase(new EndOfRoundPhase(model));
            return;
        }

        model.notifyChange("extra_draw_for:" + waitingForInput.getName());
    }

    private Player findPlayerNeedingInput() {
        for (Player p : model.getPlayers()) {
            for (PreEndOfRoundEffect effect : p.getPreEndOfRoundEffects()) {
                if (effect.needsPlayerInput()) {
                    return p;
                }
            }
        }
        return null;
    }

    @Override
    public void drawCard(int cardId) {
        // TODO: logica di pesca dalla top row per il player con ExtraCardFromTopRow
        model.getBoard().getTopRow().removeCard(cardId);
        model.notifyChange("pre_end_card_drawn:" + waitingForInput.getName());
        model.setPhase(new EndOfRoundPhase(model));
    }

    @Override
    public GamePhase getPhase() {
        return model.getCurrentPhase();
    }

    @Override
    public Player getCurrentPlayer() {
        return waitingForInput;
    }
}