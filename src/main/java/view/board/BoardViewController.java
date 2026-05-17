package view.board;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import network.client.core.LocalGameState;
import view.SceneController;
import view.SceneRouter;

/**
 * Main in-game scene controller. Owns the status bar and end-of-game
 * routing; delegates the play area and self-panel to sub-controllers
 * injected via fx:include.
 */
public class BoardViewController implements SceneController {

    @FXML private StackPane rootPane;
    @FXML private Label phaseLabel;
    @FXML private Label roundLabel;
    @FXML private Button endTurnButton;

    @FXML private BoardGameAreaController gameAreaController;
    @FXML private BoardSelfPanelController selfPanelController;

    private SceneRouter router;
    private boolean winnerShown = false;

    @Override
    public void bind(SceneRouter router) {
        this.router = router;
        gameAreaController.init(router, rootPane);
        selfPanelController.init(router, rootPane);
        LocalGameState state = router.localState();
        if (state != null && state.snapshot() != null) update(state);
    }

    @Override
    public StackPane root() { return rootPane; }

    @Override
    public void update(LocalGameState state) {
        String phase = state.getPhase();
        String me = router.playerName();
        boolean isMyTurn = me != null && me.equals(state.getCurrentPlayerName());

        updateStatusBar(state, phase, isMyTurn);
        gameAreaController.update(state);
        selfPanelController.update(state);

        if (state.isGameOver() && !winnerShown) {
            winnerShown = true;
            router.toWinner(state.getPlayers(), state.getWinners());
        }
    }

    // ── Status bar ─────────────────────────────────────────────────────────

    private void updateStatusBar(LocalGameState state, String phase, boolean isMyTurn) {
        phaseLabel.setText(formatPhase(phase));
        roundLabel.setText("Round " + state.getCurrentRound());

        boolean showEnd = "ACTION".equals(phase) && isMyTurn;
        endTurnButton.setVisible(showEnd);
        endTurnButton.setManaged(showEnd);
    }

    @FXML
    private void onEndTurn() {
        router.getVirtualServer().sendEndTurn();
    }

    private static String formatPhase(String phase) {
        if (phase == null) return "—";
        return switch (phase) {
            case "SETUP" -> "Setup";
            case "COLOR_CHOOSING_PHASE" -> "Choose color";
            case "PLACEMENT" -> "Placement";
            case "ACTION" -> "Action";
            case "PRE_END_OF_ROUND" -> "Resolving…";
            case "END_OF_ROUND" -> "End of round";
            case "END_OF_GAME" -> "End of game";
            default -> phase;
        };
    }
}
