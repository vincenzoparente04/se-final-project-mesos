package network.client.core;

import database.ScoreRecord;
import javafx.application.Platform;
import shared.dto.LobbyDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;
import view.SceneRouter;
import view.ViewController;
import view.widgets.ErrorToast;
import view.widgets.EventResolutionOverlay;

import java.util.List;

/**
 * Bridges {@link ClientStateListener} callbacks (network thread) to the JavaFX UI
 * by way of a {@link SceneRouter}. Every method jumps to the JavaFX Application Thread
 * before touching any controller.
 */
public class ClientStateListenerGui implements ClientStateListener {

    /** Navigation hub used to reach the current controller and switch scenes. */
    private final SceneRouter router;

    /**
     * @param router the scene router that resolves the current controller
     */
    public ClientStateListenerGui(SceneRouter router) {
        this.router = router;
    }

    @Override
    public void onGameStateUpdated(LocalGameState state) {
        Platform.runLater(() -> {
            // RMI reconnect
            if (router.applyPendingReconnectIfNeeded()) {
                router.currentController().update(state);
                return;
            }
            ViewController currCntrl = router.currentController();
            currCntrl.update(state);
        });
    }

    @Override
    public void onWaiting(String rawWaitingMessage) {
        // Not used by the GUI: lobby state updates are handled by onLobbyState.
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
            List<shared.dto.PlayerDto> players =
                    (router.localState() != null && router.localState().snapshot() != null)
                    ? router.localState().getPlayers()
                    : List.of();

            Runnable navigate = () -> router.toWinner(players, winners, scoring);

            // Wait for any queued event overlays (the 2 end-of-game events) to finish before navigating to the winner screen.
            EventResolutionOverlay.setOnQueueDrained(navigate);
        });
    }

    @Override
    public void onLeaderboardUpdate(List<ScoreRecord> leaderboard, int rankPosition, int points) {
        Platform.runLater(() -> router.offerLeaderboard(leaderboard, rankPosition, points));
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            ErrorToast.show(router.currentRoot(), "Disconnected from server");
            router.toNetworkSetup();
        });
    }
}
