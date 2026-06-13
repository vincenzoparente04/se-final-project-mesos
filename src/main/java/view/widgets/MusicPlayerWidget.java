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
 * Compact music-control widget displayed in the application top bar.
 * <p>
 * It renders as a single musical-note button (♪). Clicking the button toggles
 * a floating {@link Popup} panel that shows the currently playing track name and
 * exposes ⏮ / ⏯ / ⏭ transport controls together with a click-or-drag volume bar.
 * </p>
 * <p>
 * The widget wires itself to {@link MusicManager#setOnTrackChange(Runnable)} so
 * the track label and pause/play icon are kept in sync automatically whenever
 * the manager advances to a new track or the user pauses playback from another
 * call site.
 * </p>
 */
public class MusicPlayerWidget extends HBox {

    /**
     * Shared singleton that owns all audio playback state.
     * All transport actions delegate to this instance.
     */
    private final MusicManager mm = MusicManager.getInstance();

    /**
     * Floating panel that appears below the icon button when the user clicks it.
     * Auto-hides when the user clicks outside it or presses Escape.
     */
    private final Popup popup = new Popup();

    /**
     * The ⏸/▶ button inside the popup panel whose icon reflects the current
     * pause state. Updated by {@link #refreshUI()}.
     */
    private final Button pauseBtn = new Button("⏸");

    /**
     * Label inside the popup panel that shows the display name of the track
     * currently playing, or {@code "—"} when nothing is loaded.
     */
    private final Label trackLabel = new Label();

    /**
     * Visual volume indicator inside the popup. Its {@code progress} property
     * maps linearly to the {@code [0.0, 1.0]} volume range accepted by
     * {@link MusicManager#setVolume(double)}.
     */
    private final ProgressBar volBar = new ProgressBar(0.5);

    /**
     * Tracks whether the popup is currently visible, so {@link #togglePopup(Button)}
     * can decide whether to show or hide it without querying the JavaFX scene graph.
     */
    private boolean popupVisible = false;

    /**
     * Constructs the widget, adds the ♪ icon button to its layout, registers the
     * track-change callback on {@link MusicManager}, and builds the popup panel.
     * <p>
     * The constructor sets an initial spacing of 100 px (inherited from {@link HBox})
     * so that the icon button is vertically centred and does not collapse the widget
     * to zero width when used inside a toolbar with {@code HBox.setHgrow}.
     * </p>
     */
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

    /**
     * Constructs and configures the floating popup panel with its child controls:
     * the "Now Playing" title, the track-name label, the ⏮ ⏯ ⏭ control row,
     * and the volume bar. Called once from the constructor.
     */
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

    /**
     * Clamps the given raw value to {@code [0.0, 1.0]}, updates both the
     * {@link #volBar} progress indicator and the {@link MusicManager} volume.
     *
     * @param raw the unclamped volume value derived from a mouse-event x-coordinate
     *            divided by the bar width; may be negative or greater than 1.0
     */
    private void setVolume(double raw) {
        double v = Math.max(0, Math.min(1, raw));
        volBar.setProgress(v);
        mm.setVolume(v);
    }

    /**
     * Shows or hides the popup relative to the given anchor button.
     * <p>
     * When showing, the popup is positioned just below the button's bottom edge
     * (+ 4 px gap) aligned to its left edge. If the screen bounds of the anchor
     * cannot be determined (e.g. the button is not yet on screen), the popup is
     * not shown and {@link #popupVisible} is left unchanged.
     * </p>
     *
     * @param anchor the ♪ icon button used as the positioning reference
     */
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

    /**
     * Synchronises all dynamic UI elements with the current state reported by
     * {@link MusicManager}:
     * <ul>
     *   <li>{@link #trackLabel} — set to the current track name, or {@code "—"} if none</li>
     *   <li>{@link #pauseBtn} — toggled between ⏸ and ▶ based on {@link MusicManager#isPaused()}</li>
     *   <li>{@link #volBar} — re-applies the current progress value to {@link MusicManager}
     *       so the audio level stays consistent with the visual bar after a track change</li>
     * </ul>
     * <p>
     * Always called on the JavaFX Application Thread, either directly or via
     * {@link Platform#runLater(Runnable)} from the track-change callback.
     * </p>
     */
    private void refreshUI() {
        String name = mm.getCurrentTrackName();
        trackLabel.setText(name.isEmpty() ? "—" : name);
        pauseBtn.setText(mm.isPaused() ? "▶" : "⏸");
        setVolume(volBar.getProgress());
    }
}
