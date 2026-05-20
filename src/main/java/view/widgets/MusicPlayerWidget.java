package view.widgets;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import view.MusicManager;

/**
 * Small music-note button in the top bar.
 * Clicking it toggles a popup panel with ⏮ ⏯ ⏭ controls and a volume bar.
 */
public class MusicPlayerWidget extends HBox {

    private final MusicManager mm = MusicManager.getInstance();
    private final Popup popup = new Popup();
    private final Button pauseBtn = new Button("⏸");
    private final Label trackLabel = new Label();
    private final ProgressBar volBar = new ProgressBar(0.5);
    private boolean popupVisible = false;

    public MusicPlayerWidget() {
        super(100);
        setAlignment(Pos.CENTER);

        Button iconBtn = new Button("♪");
        iconBtn.getStyleClass().add("mesos-music-icon-btn");
        iconBtn.setOnAction(e -> togglePopup(iconBtn));

        mm.setOnTrackChange(() -> Platform.runLater(this::refreshUI));
        refreshUI();

        getChildren().add(iconBtn);

        buildPopup();
    }

    // ── Popup ────────────────────────────────────────────────────────────────

    private void buildPopup() {
        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.setHideOnEscape(true);
        popup.setOnHidden(e -> popupVisible = false);

        VBox panel = new VBox(10);
        panel.getStyleClass().add("mesos-dialog-panel");
        panel.setPadding(new Insets(16, 20, 16, 20));
        panel.setAlignment(Pos.CENTER);
        panel.setPrefWidth(280);

        Label title = new Label("Now Playing");
        title.getStyleClass().add("mesos-music-label");

        trackLabel.setMaxWidth(240);
        trackLabel.setWrapText(false);
        trackLabel.setAlignment(Pos.CENTER);
        trackLabel.getStyleClass().add("mesos-label-bold");

        Button prevBtn = new Button("⏮");
        pauseBtn.setText(mm.isPaused() ? "▶" : "⏸");
        Button nextBtn = new Button("⏭");
        for (Button b : new Button[]{prevBtn, pauseBtn, nextBtn})
            b.getStyleClass().add("mesos-music-btn");

        prevBtn.setOnAction(e -> mm.playPrev());
        pauseBtn.setOnAction(e -> mm.pauseResume());
        nextBtn.setOnAction(e -> mm.playNext());

        HBox controls = new HBox(12, prevBtn, pauseBtn, nextBtn);
        controls.setAlignment(Pos.CENTER);

        Label volLabel = new Label("Volume");
        volLabel.getStyleClass().add("mesos-music-label");

        volBar.setPrefWidth(200);
        volBar.getStyleClass().add("mesos-music-volume");
        volBar.setProgress(0.5);
        mm.setVolume(0.5);
        volBar.setOnMouseClicked(e -> setVolume(e.getX() / volBar.getWidth()));
        volBar.setOnMouseDragged(e -> setVolume(e.getX() / volBar.getWidth()));

        panel.getChildren().addAll(title, trackLabel, controls, volLabel, volBar);
        popup.getContent().add(panel);
    }

    private void setVolume(double raw) {
        double v = Math.max(0, Math.min(1, raw));
        volBar.setProgress(v);
        mm.setVolume(v);
    }

    private void togglePopup(Button anchor) {
        if (popupVisible) {
            popup.hide();
        } else {
            var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
            if (bounds != null) {
                refreshUI();
                popup.show(anchor, bounds.getMinX(), bounds.getMaxY() + 4);
                popupVisible = true;
            }
        }
    }

    // ── UI refresh ───────────────────────────────────────────────────────────

    private void refreshUI() {
        String name = mm.getCurrentTrackName();
        trackLabel.setText(name.isEmpty() ? "—" : name);
        pauseBtn.setText(mm.isPaused() ? "▶" : "⏸");
        setVolume(volBar.getProgress());
    }
}
