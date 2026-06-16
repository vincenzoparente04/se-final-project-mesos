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

    /** RootPane */
    @FXML private StackPane rootPane;
    /** topBar in the boardView, in which there are some button (music, rules, end turn) and some labels*/
    @FXML private HBox topBar;
    /** label in which is written the phase of the round*/
    @FXML private Label phaseLabel;
    /** label that reports the actual round over the max number of rounds*/
    @FXML private Label roundLabel;
    /** Button to end turn, enabled in specifc situation*/
    @FXML private Button endTurnButton;
    /** Button to open the rules PDF*/
    @FXML private Button rulesButton;

    /**
     * Main section of the board, dedicated to the cards on the table, other players and offerTrack and offerTiler.
     */
    @FXML private BoardGameAreaController gameAreaController;
    /**
     * The section of the board screen dedicated to the player. There are the cards and the player stats.
     */
    @FXML private BoardSelfPanelController selfPanelController;

    /**
     * The sceneRouter used to navigate between scenes.
     */
    private SceneRouter router;

    /**
     * Method to bind the Board screen with the scene router, in order to enable callback.
     * @param router actual {@link SceneRouter}
     */
    @Override
    public void bind(SceneRouter router) {
        this.router = router;
        gameAreaController.init(router, rootPane, selfPanelController);
        selfPanelController.init(router, rootPane);

        MusicPlayerWidget musicWidget = new MusicPlayerWidget();

        musicWidget.setMinHeight(36);
        musicWidget.setMaxHeight(Double.MAX_VALUE);
        int endTurnIdx = topBar.getChildren().indexOf(endTurnButton);
        topBar.getChildren().add(endTurnIdx, musicWidget);

        LocalGameState state = router.localState();
        if (state != null && state.snapshot() != null)
            Platform.runLater(() -> update(state));
    }

    /**
     * @return the root StackPane of this controller's view.
     */
    @Override
    public StackPane root() { return rootPane; }

    /**
     * Main update function of the board. It calls the {@link #updateStatusBar(LocalGameState, String, boolean)} and it calls the update
     * methods of {@link BoardGameAreaController} and {@link BoardSelfPanelController}.
     * @param state the game state given by the server
     */
    @Override
    public void update(LocalGameState state) {
        String phase = state.getPhase();
        String me = router.playerName();
        boolean isMyTurn = me != null && me.equals(state.getCurrentPlayerName());

        updateStatusBar(state, phase, isMyTurn);
        gameAreaController.update(state);
        selfPanelController.update(state);
    }

    // Status bar ---------------------------------

    /**
     * logic to update the top bar, showing or not the end of turn button, modifying the label.
     * @param state LocalGameState
     * @param phase actual phase
     * @param isMyTurn is the turn of player or not
     */
    private void updateStatusBar(LocalGameState state, String phase, boolean isMyTurn) {
        phaseLabel.setText(formatPhase(phase));
        roundLabel.setText("Round " + state.getCurrentRound());

        boolean showEnd = ("ACTION".equals(phase) || "PRE_END_OF_ROUND".equals(phase)) && isMyTurn;
        endTurnButton.setOpacity(showEnd ? 1.0 : 0.0);
        endTurnButton.setMouseTransparent(!showEnd);
    }

    /**
     * Called by clicking the onEndTurn button. It sends the command to the server of ending the round of the player.
     */
    @FXML
    private void onEndTurn() {
        router.getVirtualServer().sendEndTurn();
    }

    /**
     * Called by clicking the rules icon button. It calls the class {@link RulesOverlay} to show the overlay widget fo rules.
     */
    @FXML
    private void onOpenRules() {
        RulesOverlay.show(rootPane);
    }

    /**
     * Formatter method for displaying phases in the left hot corner
     * @param phase written in capslock eg.: "END_OF_ROUND"
     * @return pretty eg.: "End of round"
     */
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
