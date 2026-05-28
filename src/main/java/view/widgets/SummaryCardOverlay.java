package view.widgets;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Full-screen in-game overlay that shows one or more SummaryCard images.
 * When multiple images are supplied, arrow buttons allow navigating between
 * them without closing the overlay. Click anywhere on the dark backdrop to dismiss.
 */
public final class SummaryCardOverlay {

    private static final double EXPANDED_WIDTH = 700.0 * 0.75; // 525 px

    private SummaryCardOverlay() {}

    /**
     * Shows the overlay on top of {@code root}.
     * Pass one image for the original single-card behaviour, or multiple
     * images to enable arrow navigation between them.
     * Must be called on the JavaFX Application Thread.
     */
    public static void show(StackPane root, Image... images) {
        if (root == null || images == null || images.length == 0) return;

        final int[] index = {0};

        StackPane backdrop = new StackPane();
        backdrop.getStyleClass().add("mesos-card-zoom-backdrop");
        backdrop.setUserData("summary-card-overlay");

        // ── Main image ──────────────────────────────────────────────────────
        ImageView big = new ImageView(images[0]);
        big.setFitWidth(EXPANDED_WIDTH);
        big.setPreserveRatio(true);

        // ── Content panel ───────────────────────────────────────────────────
        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("mesos-card-zoom-detail");
        content.getChildren().add(big);

        // ── Navigation bar (only when more than one image) ──────────────────
        if (images.length > 1) {
            Button prev = makeArrow("‹");
            Button next = makeArrow("›");
            Label indicator = new Label();
            indicator.getStyleClass().add("mesos-hint");

            Runnable refresh = () -> {
                big.setImage(images[index[0]]);
                indicator.setText((index[0] + 1) + " / " + images.length);
                prev.setDisable(index[0] == 0);
                next.setDisable(index[0] == images.length - 1);
            };

            // Buttons consume the mouse event so it won't bubble up to the
            // backdrop and accidentally close the overlay while navigating.
            prev.setOnAction(e -> { index[0]--; refresh.run(); });
            next.setOnAction(e -> { index[0]++; refresh.run(); });

            refresh.run(); // set initial state

            HBox navRow = new HBox(16, prev, indicator, next);
            navRow.setAlignment(Pos.CENTER);
            content.getChildren().add(navRow);
        }

        backdrop.getChildren().add(content);
        backdrop.setOnMouseClicked(e -> root.getChildren().remove(backdrop));
        root.getChildren().add(backdrop);
    }

    // HELPERS

    private static Button makeArrow(String symbol) {
        Button btn = new Button(symbol);
        btn.getStyleClass().add("mesos-button-secondary");
        btn.setStyle("-fx-font-size: 18; -fx-min-width: 40; -fx-min-height: 40; -fx-padding: 0;");
        return btn;
    }
}
