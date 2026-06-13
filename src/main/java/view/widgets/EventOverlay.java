package view.widgets;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.WeakHashMap;

/**
 * Top-center transient overlay used to show event-resolution and end-game
 * scoring debug info inside the same window. Same pattern as
 * {@link ErrorToast} but centered and with a longer dwell time (8s) since
 * the body is multi-line. Non-modal and {@code pickOnBounds=false}, so it
 * never intercepts clicks or input.
 */
public final class EventOverlay {
    private static final WeakHashMap<StackPane, VBox> overlayBoxes = new WeakHashMap<>();

    private EventOverlay() {}

    public static void show(StackPane root, String title, String body) {
        if (root == null) return;
        String text = (title == null ? "" : title)
                + (body == null || body.isEmpty() ? "" : "\n" + body);

        VBox box = overlayBoxes.computeIfAbsent(root, r -> {
            VBox b = new VBox(8);
            b.setPickOnBounds(false);
            b.setMaxWidth(520);
            StackPane.setAlignment(b, Pos.TOP_CENTER);
            StackPane.setMargin(b, new Insets(40, 0, 0, 0));
            r.getChildren().add(b);
            return b;
        });

        Label label = new Label(text);
        label.getStyleClass().add("mesos-toast");
        label.setWrapText(true);
        label.setMaxWidth(500);
        box.getChildren().add(label);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), label);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition wait = new PauseTransition(Duration.seconds(8));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), label);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(fadeIn, wait, fadeOut);
        seq.setOnFinished(e -> box.getChildren().remove(label));
        seq.play();
    }
}
