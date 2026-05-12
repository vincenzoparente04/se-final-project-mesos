package network.client;

import javafx.application.Platform;
import shared.dto.LobbyDto;
import view.BoardViewController;
import view.LobbyViewController;
import view.SceneRouter;
import view.TotemPickViewController;
import view.WaitingViewController;
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
            String phase = state.getPhase();
            Object ctrl = router.currentController();

            //TODO: remove instanceof by using better the SceneRouter. Wire the boardViewController.joinSelected() to router.toBoard()
            if (ctrl instanceof BoardViewController bvc) {
                bvc.update(state);
                return;
            }
            if (ctrl instanceof TotemPickViewController tpc) {
                if (isColorChoosingPhase(phase)) {
                    tpc.update(state);
                } else {
                    router.toBoard();
                    Object after = router.currentController();
                    if (after instanceof BoardViewController bvc) bvc.update(state);
                }
                return;
            }

            // Race: a state arrived before onGameStarting routed us.
            if (isColorChoosingPhase(phase)) {
                router.toTotemPick();
                Object after = router.currentController();
                if (after instanceof TotemPickViewController tpc) tpc.update(state);
            } else {
                router.toBoard();
                Object after = router.currentController();
                if (after instanceof BoardViewController bvc) bvc.update(state);
            }
        });
    }

    @Override
    public void onWaiting(String rawWaitingMessage) {
        // TODO: server non emette eventi onWaiting al momento
    }

    @Override
    public void onLobbyList(List<LobbyDto> lobbies) {
        Platform.runLater(() -> {
            Object ctrl = router.currentController();
            if (ctrl instanceof LobbyViewController lvc) lvc.showLobbies(lobbies);
        });
    }

    @Override
    public void onLobbyState(LobbyDto lobby) {
        Platform.runLater(() -> {
            Object ctrl = router.currentController();
            if (ctrl instanceof WaitingViewController wvc) {
                wvc.update(lobby);
            } else {
                router.toWaiting(lobby);
            }
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

    private static boolean isColorChoosingPhase(String phase) {
        return "COLOR_CHOOSING_PHASE".equals(phase) || "SETUP".equals(phase);
    }
}
