package view.board;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import network.client.core.LocalGameState;
import view.SceneController;
import view.SceneRouter;
import view.widgets.MusicPlayerWidget;
import view.widgets.RulesOverlay;

/**
 * Main in-game scene controller. Owns the status bar and end-of-game
 * routing; delegates the play area and self-panel to sub-controllers
 * injected via fx:include.
 */
public class BoardViewController implements SceneController {

    @FXML private StackPane rootPane;
    @FXML private HBox topBar;
    @FXML private Label phaseLabel;
    @FXML private Label roundLabel;
    @FXML private Button endTurnButton;
    @FXML private Button rulesButton;

    @FXML private BoardGameAreaController gameAreaController;
    @FXML private BoardSelfPanelController selfPanelController;

    private SceneRouter router;
    private boolean winnerShown = false;

    @Override
    public void bind(SceneRouter router) {
        this.router = router;
        gameAreaController.init(router, rootPane, selfPanelController);
        selfPanelController.init(router, rootPane);

        // Insert MusicPlayerWidget before endTurnButton so order is: 📖 → ♪ → End turn
        MusicPlayerWidget musicWidget = new MusicPlayerWidget();
        // minHeight=36 forces the widget to overflow the topBar's 18px content area
        // (9px top/bottom padding) so its clickable area spans the full bar height,
        // matching the behaviour of rulesButton (minHeight="36" in FXML).
        musicWidget.setMinHeight(36);
        musicWidget.setMaxHeight(Double.MAX_VALUE);
        int endTurnIdx = topBar.getChildren().indexOf(endTurnButton);
        topBar.getChildren().add(endTurnIdx, musicWidget);

        LocalGameState state = router.localState();
        if (state != null && state.snapshot() != null)
            Platform.runLater(() -> update(state));
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
        endTurnButton.setOpacity(showEnd ? 1.0 : 0.0);
        endTurnButton.setMouseTransparent(!showEnd);
    }

    @FXML
    private void onEndTurn() {
        router.getVirtualServer().sendEndTurn();
    }

    @FXML
    private void onOpenRules() {
        RulesOverlay.show(rootPane);
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
