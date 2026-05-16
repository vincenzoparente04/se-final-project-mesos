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
import javafx.stage.Window;
import shared.dto.CardDto;
import shared.dto.PlayerDto;
import view.widgets.CardView;
import view.widgets.CardZoomOverlay;
import view.widgets.TotemView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modal popup that shows another player's full tribe.
 * Characters are grouped by sub-type (HUNTER, BUILDER, …) and each card carries
 * a short caption with the key info (e.g. star count for shamans). Click any card
 * to zoom it.
 */
public class TribePopupController implements ViewController {

    @FXML private HBox  titleBar;
    @FXML private Label titleLabel;
    @FXML private Label statsLabel;
    @FXML private VBox  charactersBox;
    @FXML private VBox  buildingsBox;

    private Stage stage;
    /** A separate root we attach card-zoom overlays to. */
    private StackPane overlayRoot;

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

            // Center on owner window
            st.setX(owner.getX() + owner.getWidth()  / 2 - 420);
            st.setY(owner.getY() + owner.getHeight() / 2 - 300);
            st.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load tribe-popup.fxml", e);
        }
    }

    private void init(PlayerDto target, Parent root) {
        overlayRoot = (StackPane) root;

        titleLabel.setGraphic(new TotemView(target.color, 28));
        titleLabel.setText("  " + target.name);

        statsLabel.setText("food  " + target.food + "    ·    PP  " + target.prestigePoints);

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
            charactersBox.getChildren().add(buildGroup(prettyType(e.getKey()), e.getValue()));
        }
    }

    private void renderBuildings(PlayerDto target) {
        buildingsBox.getChildren().clear();
        if (target.tribe == null || target.tribe.buildings == null
                || target.tribe.buildings.isEmpty()) {
            return;
        }
        buildingsBox.getChildren().add(buildGroup("Buildings", target.tribe.buildings));
    }

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

            String meta = buildCardMeta(c);
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

    private static String buildCardMeta(CardDto c) {
        StringBuilder sb = new StringBuilder();
        if (c.details != null && !c.details.isBlank()) sb.append(c.details);
        if (c.foodCost > 0) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append("cost ").append(c.foodCost);
        }
        if (c.endGamePoints > 0) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append("+").append(c.endGamePoints).append("PP");
        }
        return sb.toString();
    }

    private static String prettyType(String type) {
        if (type == null) return "?";
        return switch (type) {
            case "HUNTER"   -> "Hunters";
            case "BUILDER"  -> "Builders";
            case "SHAMAN"   -> "Shamans";
            case "ARTIST"   -> "Artists";
            case "INVENTOR" -> "Inventors";
            case "GATHERER" -> "Gatherers";
            default         -> type;
        };
    }

    @FXML
    private void onClose() {
        if (stage != null) stage.close();
    }
}
