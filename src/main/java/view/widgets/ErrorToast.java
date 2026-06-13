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
 * Top-right transient notification.
 * All toasts for a given root share a single VBox anchored to the top-right corner;
 * vertical stacking is handled by the VBox layout, not by manual offset calculation.
 */
public final class ErrorToast {
    /**
     * weak hash map of stackPane and Vbox of toast boxes.
     */
    private static final WeakHashMap<StackPane, VBox> toastBoxes = new WeakHashMap<>();

    /**
     * no
     */
    private ErrorToast() {}

    /**
     * main method of the class that shows the error notify toast.
     * @param root stackpane on which to show the nottifications.
     * @param message error message
     */
    public static void show(StackPane root, String message) {
        if (root == null || message == null) return;

        VBox toastBox = toastBoxes.computeIfAbsent(root, r -> {
            VBox box = new VBox(8);
            box.setPickOnBounds(false);
            box.setMaxWidth(380);
            StackPane.setAlignment(box, Pos.TOP_RIGHT);
            StackPane.setMargin(box, new Insets(16, 20, 0, 0));
            r.getChildren().add(box);
            return box;
        });

        Label label = new Label(message);
        label.getStyleClass().add("mesos-toast");
        label.setWrapText(true);
        label.setMaxWidth(360);
        toastBox.getChildren().add(label);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), label);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition wait = new PauseTransition(Duration.seconds(3.2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(420), label);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(fadeIn, wait, fadeOut);
        seq.setOnFinished(e -> toastBox.getChildren().remove(label));
        seq.play();
    }
}
