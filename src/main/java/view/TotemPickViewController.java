package view;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import network.client.core.LocalGameState;
import shared.dto.PlayerDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shown after the lobby fills and before the board appears.
 * The user picks a totem colour; colours already taken by others are disabled.
 * When the server moves past COLOR_CHOOSING_PHASE the listener routes to the board.
 */
public class TotemPickViewController implements SceneController {

    private static final String[] COLORS = {"RED", "BLUE", "GREEN", "YELLOW", "WHITE"};

    @FXML private StackPane rootPane;
    @FXML private HBox colorRow;
    @FXML private Label hintLabel;
    @FXML private Label statusLabel;
    @FXML private VBox othersBox;

    private SceneRouter router;
    private final List<Button> colorButtons = new ArrayList<>();

    public void bind(SceneRouter router) {
        this.router = router;
        buildButtons();

        LocalGameState state = router.localState();
        if (state != null && state.snapshot() != null) {
            update(state);
        } else {
            hintLabel.setText("Waiting for the colour-pick phase to start…");
        }
    }

    public StackPane root() { return rootPane; }

    public void update(LocalGameState state) {
        String phase = state.getPhase();
        if(!("COLOR_CHOOSING_PHASE".equals(phase))){
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

        for (Button btn : colorButtons()) {
            String color = (String) btn.getUserData();
            boolean alreadyTaken = taken.contains(color);
            //TODO: adjust the disabled totem buttons to match the concurrent color choosing phase
            btn.setDisable(!isMyTurn || iHavePicked || alreadyTaken);
            //btn.setDisable(alreadyTaken);
        }

        //TODO: remove the last else after the concurring color choosing phase
        if (iHavePicked) {
            hintLabel.setText("You picked " + self.color + ". Waiting for the others…");
        } else if (isMyTurn) {
            hintLabel.setText("Pick a colour for your totem.");
        } else {
            String cp = state.getCurrentPlayerName();
            hintLabel.setText(cp != null ? cp + " is picking a colour…" : "Waiting…");
        }

        statusLabel.setText("");
    }

    private void buildButtons() {
        for (String c : COLORS) {
            Button btn = new Button();
            btn.setUserData(c);

            String imagePath = "/images/totems/front/totem_front_" + c.toLowerCase() + ".png";
            java.io.InputStream stream = getClass().getResourceAsStream(imagePath);
            if (stream != null) {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(new javafx.scene.image.Image(stream));
                iv.setFitHeight(120);
                iv.setPreserveRatio(true);
                btn.setGraphic(iv);
                btn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            } else {
                btn.getStyleClass().addAll("mesos-totem-btn", "mesos-totem-" + c);
            }
            
            btn.setOnAction(e -> {
                router.getVirtualServer().sendChooseColor(c);
                //TODO: uncomment it when the color choosing phase will handle concurrency
                //btn.setDisable(true);
            });
            colorButtons.add(btn);
            colorRow.getChildren().add(btn);
        }
        HBox.setMargin(colorRow, null);
        colorRow.setAlignment(Pos.CENTER);
    }

    private List<Button> colorButtons() {
        return colorButtons;
    }
}
