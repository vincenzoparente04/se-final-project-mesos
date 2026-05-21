package view.widgets;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Full-screen in-game overlay that shows the SummaryCard image at readable size.
 * Identical structure to {@link CardZoomOverlay}: backdrop + mesos-card-zoom-detail box.
 * Click anywhere to dismiss.
 */
public final class SummaryCardOverlay {

    private static final double EXPANDED_WIDTH = 700.0 * 0.75; // 525 px

    private SummaryCardOverlay() {}

    /**
     * Shows the SummaryCard overlay on top of {@code root}.
     * Must be called on the JavaFX Application Thread.
     */
    public static void show(StackPane root, Image img) {
        if (root == null || img == null) return;

        StackPane backdrop = new StackPane();
        backdrop.getStyleClass().add("mesos-card-zoom-backdrop");
        backdrop.setUserData("summary-card-overlay");

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("mesos-card-zoom-detail");

        ImageView big = new ImageView(img);
        big.setFitWidth(EXPANDED_WIDTH);
        big.setPreserveRatio(true);
        content.getChildren().add(big);

        backdrop.getChildren().add(content);
        backdrop.setOnMouseClicked(e -> root.getChildren().remove(backdrop));
        root.getChildren().add(backdrop);
    }
}
