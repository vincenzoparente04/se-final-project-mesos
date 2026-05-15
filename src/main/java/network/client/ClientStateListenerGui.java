package network.client;

import javafx.application.Platform;
import shared.dto.LobbyDto;
import view.SceneRouter;
import view.ViewController;
import view.widgets.ErrorToast;

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
        // TODO: server non emette eventi onWaiting al momento
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
    public void onGameOver(List<String> winners) {
        Platform.runLater(() -> {
            if (router.localState() == null || router.localState().snapshot() == null) {
                router.toWinner(List.of(), winners);
                return;
            }
            router.toWinner(router.localState().getPlayers(), winners);
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> ErrorToast.show(router.currentRoot(), "Disconnected from server"));
    }
}
