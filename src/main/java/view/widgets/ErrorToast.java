package view.widgets;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Top-right transient notification.
 * Stacks new toasts vertically; each fades out after a short delay.
 */
public final class ErrorToast {

    private ErrorToast() {}

    public static void show(StackPane root, String message) {
        if (root == null || message == null) return;

        Label label = new Label(message);
        label.setStyle(
                "-fx-background-color: rgba(192, 57, 43, 0.92);" +
                "-fx-text-fill: white;" +
                "-fx-padding: 10 16 10 16;" +
                "-fx-background-radius: 6;" +
                "-fx-font-size: 13;" +
                "-fx-font-weight: bold;");
        label.setWrapText(true);
        label.setMaxWidth(360);
        StackPane.setAlignment(label, Pos.TOP_RIGHT);
        StackPane.setMargin(label, new Insets(16 + countExistingToasts(root) * 56, 20, 0, 0));

        root.getChildren().add(label);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), label);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition wait = new PauseTransition(Duration.seconds(3.2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(420), label);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(fadeIn, wait, fadeOut);
        seq.setOnFinished(e -> root.getChildren().remove(label));
        seq.play();
    }

    private static int countExistingToasts(StackPane root) {
        return (int) root.getChildren().stream()
                .filter(n -> n instanceof Label
                        && "error-toast".equals(n.getUserData()))
                .count();
    }
}
