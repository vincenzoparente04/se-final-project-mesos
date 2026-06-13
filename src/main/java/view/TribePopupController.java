package view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import view.widgets.CardFormatter;
import javafx.stage.Window;
import shared.dto.CardDto;
import shared.dto.PlayerDto;
import view.widgets.CardView;
import view.widgets.CardZoomOverlay;
import view.widgets.FoodWidget;
import view.widgets.PpWidget;
import view.widgets.TotemView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Popup that shows another player's full tribe.
 * Characters are grouped by type (HUNTER, BUILDER, …) and each card carries
 * a short caption with the key info (e.g. star count for shamans). Click any card
 * to zoom it.
 */
public class TribePopupController implements ViewController {

    /**Title HBox bar */
    @FXML private HBox  titleBar;
    /** Title Label */
    @FXML private Label titleLabel;
    /** HBox containing the food and PP widgets */
    @FXML private HBox  statsBox;
    /** VBox for the character cards, grouped by type */
    @FXML private VBox  charactersBox;
    /** VBox for the building cards*/
    @FXML private VBox  buildingsBox;
    
    /** The window stage representing this popup */
    private Stage stage;
    /** The root pane containing the popup layout */
    private StackPane overlayRoot;

    private static Stage openStage = null;

    /**
     * Called by {@link view.widgets.EventResolutionOverlay}.
     * <p>
     * When an event is resolved, if the tribe popup is open, it's automatically closed to avoid having two popups opened together.
     */
    public static void closeIfOpen() {
        if (openStage != null) {
            openStage.close();
            openStage = null;
        }
    }

    /**
     * Main method of the {@link TribePopupController}. Called when the user clicks the summary panel of another player.
     * It loads the FXML and appends it to the board view.
     * @param owner the window that called the tribePopupController
     * @param target Player of which the data appears.
     */
    public static void show(Window owner, PlayerDto target) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    TribePopupController.class.getResource("/org/example/mesos/tribe-popup.fxml"));
            Parent root = loader.load();
            TribePopupController ctrl = loader.getController();
            ctrl.init(target, root);

            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.initOwner(owner);
            st.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(ctrl.overlayRoot);
            scene.setFill(Color.TRANSPARENT);
            scene.getStylesheets().add(
                    TribePopupController.class.getResource("/styles/mesos.css").toExternalForm());
            st.setScene(scene);
            ctrl.stage = st;
            openStage = st;
            st.setOnHidden(e -> { if (openStage == st) openStage = null; });

            // Center on owner window
            st.setX(owner.getX() + owner.getWidth()  / 2 - 420);
            st.setY(owner.getY() + owner.getHeight() / 2 - 300);
            st.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load tribe-popup.fxml", e);
        }
    }

    /**
     * Initializes the popup with target player data and the root graphical element.
     * 
     * @param target Player of which the data is displayed.
     * @param root The root parent node of the popup.
     */
    private void init(PlayerDto target, Parent root) {
        overlayRoot = (StackPane) root;

        titleLabel.setGraphic(new TotemView(target.color, 28));
        titleLabel.setText("  " + target.name);

        statsBox.getChildren().setAll(new FoodWidget(target.food), new PpWidget(target.prestigePoints));

        // Drag support — move the window by dragging the title bar
        final double[] drag = new double[2];
        titleBar.setOnMousePressed(e -> {
            drag[0] = overlayRoot.getScene().getWindow().getX() - e.getScreenX();
            drag[1] = overlayRoot.getScene().getWindow().getY() - e.getScreenY();
        });
        titleBar.setOnMouseDragged(e -> {
            overlayRoot.getScene().getWindow().setX(e.getScreenX() + drag[0]);
            overlayRoot.getScene().getWindow().setY(e.getScreenY() + drag[1]);
        });

        renderCharacters(target);
        renderBuildings(target);
    }

    /**
     * Called by {@code init()}. It renders the characters cards in the Tribe Popup
     * @param target the DTO of the player that 'owns' the popup
     */
    private void renderCharacters(PlayerDto target) {
        charactersBox.getChildren().clear();
        if (target.tribe == null || target.tribe.characterCards == null
                || target.tribe.characterCards.isEmpty()) {
            return;
        }

        Map<String, List<CardDto>> grouped = new LinkedHashMap<>();
        for (String t : new String[]{"HUNTER", "BUILDER", "SHAMAN", "ARTIST", "INVENTOR", "GATHERER"}) {
            grouped.put(t, new ArrayList<>());
        }
        for (CardDto c : target.tribe.characterCards) {
            grouped.computeIfAbsent(c.type, k -> new ArrayList<>()).add(c);
        }

        for (Map.Entry<String, List<CardDto>> e : grouped.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            charactersBox.getChildren().add(buildGroup(CardFormatter.prettyType(e.getKey()), e.getValue()));
        }
    }

    /**
     * Called by {@code init()}. It renders the building cards in the Tribe Popup
     * @param target the DTO of the player that 'owns' the popup
     */
    private void renderBuildings(PlayerDto target) {
        buildingsBox.getChildren().clear();
        if (target.tribe == null || target.tribe.buildings == null
                || target.tribe.buildings.isEmpty()) {
            return;
        }
        buildingsBox.getChildren().add(buildGroup("Buildings", target.tribe.buildings));
    }

    /**
     * Called by {@code renderCharacters()} and {@code renderBuildings()}.
     * It builds a VBox containing a group of characters cards or building cards.
     * @param title The name of the type of the group of cards
     * @param cards the DTO of cards
     * @return VBox made of sequential cards of the group
     */
    private VBox buildGroup(String title, List<CardDto> cards) {
        VBox group = new VBox(8);
        group.getStyleClass().add("mesos-tribe-group");

        Label header = new Label(title + "   ×" + cards.size());
        header.getStyleClass().add("mesos-tribe-group-title");
        group.getChildren().add(header);

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);

        for (CardDto c : cards) {
            VBox cell = new VBox(4);
            cell.setAlignment(Pos.CENTER);
            CardView v = new CardView(c, true, 86, 124);
            v.setStyle("-fx-cursor: hand;");
            v.setOnMouseClicked(e -> CardZoomOverlay.show(overlayRoot, c));
            cell.getChildren().add(v);

            String meta = CardFormatter.buildCardMeta(c);
            if (meta != null && !meta.isBlank()) {
                Label m = new Label(meta);
                m.getStyleClass().add("mesos-card-meta");
                m.setWrapText(true);
                m.setMaxWidth(120);
                m.setAlignment(Pos.CENTER);
                cell.getChildren().add(m);
            }
            row.getChildren().add(cell);
        }
        group.getChildren().add(row);
        return group;
    }

    /**
     * Called when the button "close" is clicked.
     * Close the popup window.
     */
    @FXML
    private void onClose() {
        if (stage != null) stage.close();
    }
}
