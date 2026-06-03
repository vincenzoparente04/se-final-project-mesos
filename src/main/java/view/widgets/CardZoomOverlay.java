package view.widgets;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import shared.dto.CardDto;

/**
 * Translucent overlay that shows a single card at large size on top of the current scene.
 * Click anywhere on the backdrop (or the card itself) to dismiss.
 */
public final class CardZoomOverlay {

    private CardZoomOverlay() {}

    public static void show(StackPane root, CardDto card) {
        if (root == null || card == null) return;

        StackPane backdrop = new StackPane();
        backdrop.getStyleClass().add("mesos-card-zoom-backdrop");
        backdrop.setUserData("card-zoom-overlay");

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("mesos-card-zoom-detail");

        CardView big = new CardView(card, true, 280, 396);
        content.getChildren().add(big);

        Label type = new Label(CardFormatter.formatType(card));
        type.getStyleClass().add("mesos-label-bold");
        content.getChildren().add(type);

        String meta = CardFormatter.buildMetaText(card);
        if (meta != null && !meta.isBlank()) {
            Label metaLbl = new Label(meta);
            metaLbl.getStyleClass().add("mesos-card-meta");
            metaLbl.setWrapText(true);
            metaLbl.setMaxWidth(320);
            content.getChildren().add(metaLbl);
        }

        backdrop.getChildren().add(content);
        backdrop.setOnMouseClicked(e -> root.getChildren().remove(backdrop));
        root.getChildren().add(backdrop);
    }
}
