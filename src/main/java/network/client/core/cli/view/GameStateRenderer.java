package network.client.core.cli.view;

import shared.dto.GameStateDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;

import java.util.List;

/**
 * interfaccia utilizzata per delegare la visualizzazione, clientStateListener viola
 * SRP se lo fa lui.
 */

// TODO: cambiare il paramentro in modo da passare il dto e non il localGameState
public interface GameStateRenderer {
    void render(GameStateDto state, String localPlayerName);
    void renderAllTribes(GameStateDto dto, String localPlayerName);

    /** Render the per-player breakdown of a single resolved event card. */
    void renderEvent(EventResolutionDto resolution, String localPlayerName);

    /** Render the end-game scoring breakdown (5 voci per player) plus the winners line. */
    void renderEndGameScoring(EndGameScoringDto scoring, List<String> winners, String localPlayerName);
}
