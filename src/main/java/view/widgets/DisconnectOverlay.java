package view.widgets;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Full-screen overlay shown when the connection to the server is lost.
 * Counts down from {@value #COUNTDOWN_SECONDS} seconds, then closes the application.
 */
public final class DisconnectOverlay {

    private static final int COUNTDOWN_SECONDS = 10;

    private DisconnectOverlay() {}

    /**
     * @param root  the root {@link StackPane} of the current scene
     * @param stage the primary stage — will be closed when the countdown reaches zero
     */
    public static void show(StackPane root, Stage stage) {
        if (root == null || stage == null) return;

        StackPane backdrop = new StackPane();
        backdrop.getStyleClass().add("mesos-disconnect-backdrop");

        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("mesos-disconnect-box");

        Label titleLabel = new Label("Connessione persa");
        titleLabel.getStyleClass().add("mesos-disconnect-title");

        Label countdownLabel = new Label();
        countdownLabel.getStyleClass().add("mesos-disconnect-countdown");

        box.getChildren().addAll(titleLabel, countdownLabel);
        backdrop.getChildren().add(box);
        root.getChildren().add(backdrop);

        int[] remaining = {COUNTDOWN_SECONDS};
        countdownLabel.setText(buildMessage(remaining[0]));

        Timeline timeline = new Timeline();
        timeline.setCycleCount(COUNTDOWN_SECONDS);
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0]--;
            countdownLabel.setText(buildMessage(remaining[0]));
            if (remaining[0] <= 0) {
                Platform.runLater(stage::close);
            }
        }));
        timeline.play();
    }

    private static String buildMessage(int seconds) {
        if (seconds <= 0) return "Chiusura in corso…";
        return "Ti disconnetterai tra: " + seconds;
    }
}
