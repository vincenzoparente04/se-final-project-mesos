package network.client.core.cli.view;

import network.client.core.LocalGameState;
import shared.dto.GameStateDto;

/**
 * interfaccia utilizzata per delegare la visualizzazione, clientStateListener viola
 * SRP se lo fa lui. da utilizzare anche per la view
 */

// TODO: cambiare il paramentro in modo da passare il dto e non il localGameState
public interface GameStateRenderer {
    void render(GameStateDto state, String localPlayerName);
    void renderAllTribes(GameStateDto dto, String localPlayerName);
}
