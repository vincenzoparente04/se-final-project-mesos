package model.buildingEffects.PreEndOfRoundEffects;

import model.player.Player;

public class ExtraDrawEffect extends PreEndOfRoundEffect {
    @Override
    public boolean needsPlayerInput() { return true; }

    @Override
    public void applyEffect(Player player) {
        // non usato — la logica di pesca sta nella fase
    }
}
