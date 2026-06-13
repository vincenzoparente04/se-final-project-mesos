package view;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import network.client.core.LocalGameState;
import shared.dto.PlayerDto;
import view.widgets.ImageCache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shown after the lobby fills and before the board appears.
 * The user picks a totem color; colors already taken by others are disabled.
 * When the server moves past COLOR_CHOOSING_PHASE the listener routes to the board.
 */
public class TotemPickViewController implements SceneController {
    /**List of the possible colors*/
    private static final String[] COLORS = {"RED", "BLUE", "PURPLE", "YELLOW", "WHITE"};
    /** The root pane of the totemPickView screen layout. */
    @FXML private StackPane rootPane;
    /** HBox containing the totems of different colors*/
    @FXML private HBox colorRow;
    /** Text label to show the state of color choosing and various messages*/
    @FXML private Label hintLabel;

    @FXML private VBox othersBox;

    /** The router used to navigate between scenes. */
    private SceneRouter router;
    /** List of totems of different colors. All totems are buttons that are clickable */
    private final List<Button> colorButtons = new ArrayList<>();

    /**
     * Binds the {@link SceneRouter} to this controller, calls the {@code buildButtons()}, checks if the gameState is arrived
     * from the server: if positive calls {@code update()}, if negative sets the hint label to "Waiting for color choosing phase to start...".
     * @param router actual {@link SceneRouter}
     */
    public void bind(SceneRouter router) {
        this.router = router;
        buildButtons();

        LocalGameState state = router.localState();
        if (state != null && state.snapshot() != null) {
            update(state);
        } else {
            hintLabel.setText("Waiting for the color choosing phase to start…");
        }
    }

    /**
     * Returns the root node of this controller's view.
     */
    @Override
    public StackPane root() { return rootPane; }

    /**
     * Updates the GUI based on the current local game state received from the server.
     * <p>
     * It recalculates the availability of each color by checking which ones have already been picked by other players.
     * It updates the labels and button states to guide the player (or tell them to wait)
     * according to whether it's currently their turn and whether they've already picked.
     *</p>
     * <p>
     *     Otherwise, if the game phase is not color choosing phase, it redirects the application
     *      to the main game board, ending this phase.
     * </p>
     * @param state the current local game state containing player and phase information
     */
    public void update(LocalGameState state) {
        String phase = state.getPhase();
        if (!("COLOR_CHOOSING_PHASE".equals(phase))) {
            router.toBoard();
            return;
        }
        String me = router.playerName();
        boolean isMyTurn = me != null && me.equals(state.getCurrentPlayerName());

        Set<String> taken = new HashSet<>();
        PlayerDto self = null;
        for (PlayerDto p : state.getPlayers()) {
            if (p.color != null) taken.add(p.color);
            if (p.name.equals(me)) self = p;
        }

        boolean iHavePicked = self != null && self.color != null;

        for (Button btn : colorButtons) {
            String color = (String) btn.getUserData();
            boolean alreadyTaken = taken.contains(color);
            btn.setDisable(alreadyTaken);
            btn.setMouseTransparent(iHavePicked);
            btn.setFocusTraversable(!iHavePicked);
        }

        if (iHavePicked) {
            hintLabel.setText("Waiting for the others…");
        } else if (isMyTurn) {
            hintLabel.setText("Pick a colour for your totem.");
        }
    }

    /**
     * Constructs and initializes the color picking buttons.
     * <p>
     * Iterates over the available predefined colors, retrieves the corresponding totem images
     * from the cache, and creates a button for each. It also attaches an action event
     * to each button so that clicking it sends the chosen color to the server and disables it.
     */
    private void buildButtons() {
        for (String c : COLORS) {
            Button btn = new Button();
            btn.setUserData(c);

            String imagePath = "/images/totems/front/totem_front_" + c.toLowerCase() + ".png";
            Image img = ImageCache.get(imagePath);
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitHeight(120);
                iv.setPreserveRatio(true);
                btn.setGraphic(iv);
                btn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            } else {
                btn.getStyleClass().addAll("mesos-totem-btn", "mesos-totem-" + c);
            }

            btn.setOnAction(e -> {
                router.getVirtualServer().sendChooseColor(c);
                btn.setDisable(true);
            });
            colorButtons.add(btn);
            colorRow.getChildren().add(btn);
        }
        HBox.setMargin(colorRow, null);
        colorRow.setAlignment(Pos.CENTER);
    }
}
