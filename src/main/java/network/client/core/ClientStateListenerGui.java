package network.client.core;

import javafx.application.Platform;
import shared.dto.LobbyDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;
import shared.dto.event.PlayerScoringDeltaDto;
import view.SceneRouter;
import view.ViewController;
import view.widgets.ErrorToast;
import view.widgets.EventOverlay;
import view.widgets.EventResolutionOverlay;

import java.util.List;

/**
 * Bridges {@link ClientStateListener} callbacks (network thread) to the JavaFX UI
 * by way of a {@link SceneRouter}. Every method jumps to the JavaFX Application Thread
 * before touching any controller.
 */
public class ClientStateListenerGui implements ClientStateListener {

    private final SceneRouter router;

    public ClientStateListenerGui(SceneRouter router) {
        this.router = router;
    }

    @Override
    public void onGameStateUpdated(LocalGameState state) {
        Platform.runLater(() -> {
            ViewController currCntrl = router.currentController();
            currCntrl.update(state);
        });
    }

    @Override
    public void onWaiting(String rawWaitingMessage) {
        // TODO: a che serve? cosa fa che non si può fare con onLobbyState?
    }

    @Override
    public void onLobbyList(List<LobbyDto> lobbies) {
        Platform.runLater(() -> {
            ViewController currController = router.currentController();
            currController.showLobbies(lobbies);
        });
    }

    @Override
    public void onLobbyState(LobbyDto lobby) {
        Platform.runLater(() -> {
            ViewController currController = router.currentController();
            currController.showLobbyState(lobby);
        });
    }

    @Override
    public void onGameStarting() {
        Platform.runLater(router::toTotemPick);
    }

    @Override
    public void onError(String message) {
        Platform.runLater(() -> ErrorToast.show(router.currentRoot(), message));
    }

    @Override
    public void onEventResolved(EventResolutionDto resolution) {
        if (resolution == null) return;
        Platform.runLater(() -> EventResolutionOverlay.enqueue(router.currentRoot(), resolution));
    }

    @Override
    public void onGameOver(List<String> winners, EndGameScoringDto scoring) {
        Platform.runLater(() -> {
            // Debug overlay with the end-game scoring breakdown (if any).
            // Will be replaced by a proper end-game screen by the UI team.
            if (scoring != null) {
                String header = (winners == null || winners.isEmpty())
                        ? "GAME OVER — no winners"
                        : "GAME OVER — Winner(s): " + String.join(", ", winners);
                EventOverlay.show(router.currentRoot(), header, formatScoring(scoring));
            }

            if (router.localState() == null || router.localState().snapshot() == null) {
                router.toWinner(List.of(), winners);
                return;
            }
            router.toWinner(router.localState().getPlayers(), winners);
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            ErrorToast.show(router.currentRoot(), "Disconnected from server");
            router.toNetworkSetup();
        });
    }

    // ─── Formatters (debug-quality, line-based) ─────────────────────────

    private static String formatScoring(EndGameScoringDto scoring) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-12s  %4s %4s %4s %4s %4s   %4s → %4s%n",
                "Player", "Bld", "Art", "Inv", "BPP", "Eff", "PP", "PP"));
        for (PlayerScoringDeltaDto d : scoring.deltas) {
            sb.append(String.format("%-12s  %+4d %+4d %+4d %+4d %+4d   %4d → %4d%n",
                    d.playerName,
                    d.buildersPoints, d.artistsPoints, d.inventorsPoints,
                    d.buildingPrintedPoints, d.endGameBuildingEffectsPoints,
                    d.prestigeBefore, d.prestigeAfter));
        }
        return sb.toString();
    }
}
