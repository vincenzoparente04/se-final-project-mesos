package view.widgets;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import shared.dto.event.EventResolutionDto;
import shared.dto.event.PlayerEventDeltaDto;
import view.TribePopupController;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Themed modal overlay that presents event-resolution results one at a time.
 * <p>
 * Multiple events can be enqueued via {@link #enqueue(StackPane, EventResolutionDto)};
 * each is shown for {@value #DWELL_SECONDS} seconds with a visible countdown bar,
 * then the next is displayed automatically. Clicking anywhere on the backdrop
 * advances to the next event immediately.
 * </p>
 * <p>
 * The class is entirely static — it manages a single application-wide overlay
 * state and cannot be instantiated.
 * </p>
 */
public final class EventResolutionOverlay {

    /**
     * Number of seconds each event card stays visible before automatically
     * advancing to the next one. Also drives the {@link ProgressBar} countdown.
     */
    private static final double DWELL_SECONDS = 12.0;

    /**
     * Duration in milliseconds of the fade-in animation played when a new
     * event card appears on screen.
     */
    private static final double FADE_IN_MS = 300;

    /**
     * Duration in milliseconds of the fade-out animation played when an
     * event card is dismissed before the next one is shown.
     */
    private static final double FADE_OUT_MS = 200;

    /**
     * Immutable container pairing an event card's root {@link StackPane} with
     * the data needed to render it. Stored in {@link #queue} until displayed.
     *
     * @param root the scene-graph node that the backdrop will be added to
     * @param dto the event data to display
     */
    private record PendingEvent(StackPane root, EventResolutionDto dto) {}

    /**
     * FIFO queue of events waiting to be shown. Events are added by
     * {@link #enqueue(StackPane, EventResolutionDto)} and consumed by
     * {@link #showNext()}.
     */
    private static final Deque<PendingEvent> queue = new ArrayDeque<>();

    /**
     * The {@link StackPane} scene root that currently hosts the active backdrop,
     * or {@code null} when no overlay is visible.
     */
    private static StackPane activeRoot = null;

    /**
     * The backdrop {@link StackPane} currently overlaying the scene, or
     * {@code null} when no overlay is visible. Removed from {@link #activeRoot}
     * after the fade-out completes.
     */
    private static StackPane activeBackdrop = null;

    /**
     * The {@link PauseTransition} that fires {@link #advance()} after
     * {@link #DWELL_SECONDS} seconds. Stopped and nulled whenever the overlay
     * is dismissed early (by a click or {@link #reset()}).
     */
    private static PauseTransition activeDwell = null;

    /**
     * The {@link Timeline} that animates the {@link ProgressBar} from full to
     * empty over {@link #DWELL_SECONDS} seconds. Started by {@link #startDwell()}
     * after the fade-in completes, so the bar and the dwell timer stay in sync.
     */
    private static Timeline activeProgress = null;

    /**
     * One-shot callback invoked on the JavaFX Application Thread when the last
     * overlay closes and {@link #queue} becomes empty. Cleared after firing to
     * prevent double invocation.
     */
    private static Runnable onQueueDrained = null;

    /**
     * Private constructor — this class is a static utility and must not be instantiated.
     */
    private EventResolutionOverlay() {}

    //Public API

    /**
     * Returns {@code true} when there are no events currently visible and no
     * events waiting in the queue.
     *
     * @return {@code true} if the overlay system is fully idle
     */
    public static boolean isQueueEmpty() {
        return queue.isEmpty() && activeBackdrop == null;
    }

    /**
     * Registers a one-shot callback that fires on the JavaFX Application Thread
     * when the last overlay closes and the queue drains to empty.
     * <p>
     * If the queue is already empty at the time of registration, the callback
     * is invoked immediately rather than being stored.
     * </p>
     *
     * @param callback the {@link Runnable} to invoke when all overlays have been dismissed
     */
    public static void setOnQueueDrained(Runnable callback) {
        if (isQueueEmpty()) { callback.run(); return; }
        onQueueDrained = callback;
    }

    /**
     * Adds an event to the display queue and starts showing it immediately if
     * no other overlay is currently active. Silently ignores {@code null} arguments.
     *
     * @param root the {@link StackPane} scene root that will host the backdrop
     * @param dto  the resolved-event data to display
     */
    public static void enqueue(StackPane root, EventResolutionDto dto) {
        if (root == null || dto == null) return;
        queue.add(new PendingEvent(root, dto));
        if (activeBackdrop == null) showNext();
    }

    /**
     * Immediately stops all animations, removes the active backdrop from the scene,
     * clears the pending queue, and resets all static state to its initial values.
     * <p>
     * Any registered {@link #onQueueDrained} callback is discarded without firing.
     * </p>
     */
    public static void reset() {
        if (activeDwell != null) { activeDwell.stop(); activeDwell = null; }
        if (activeProgress != null) { activeProgress.stop(); activeProgress = null; }
        if (activeBackdrop != null && activeRoot != null)
            activeRoot.getChildren().remove(activeBackdrop);
        queue.clear();
        activeRoot = null;
        activeBackdrop = null;
        onQueueDrained = null;
    }

    // Internal flow

    /**
     * Cancels the current dwell timer and progress animation, then fades out the
     * active backdrop before calling {@link #showNext()}. If no backdrop is visible,
     * calls {@link #showNext()} directly.
     * <p>
     * Invoked either by the dwell timer expiring or by a mouse click on the backdrop.
     * </p>
     */
    private static void advance() {
        if (activeDwell != null) { activeDwell.stop(); activeDwell = null; }
        if (activeProgress != null) { activeProgress.stop(); activeProgress = null; }

        if (activeBackdrop != null && activeRoot != null) {
            StackPane bd = activeBackdrop;
            StackPane root = activeRoot;
            activeBackdrop = null;
            FadeTransition out = new FadeTransition(Duration.millis(FADE_OUT_MS), bd);
            out.setFromValue(1.0);
            out.setToValue(0.0);
            out.setOnFinished(e -> { root.getChildren().remove(bd); showNext(); });
            out.play();
        } else {
            showNext();
        }
    }

    /**
     * Polls the next {@link PendingEvent} from {@link #queue} and displays it.
     * <p>
     * If the queue is empty, clears {@link #activeRoot} and fires
     * {@link #onQueueDrained} (if registered). Otherwise:
     * <ol>
     *   <li>Closes any open {@link TribePopupController} popup.</li>
     *   <li>Builds the visual panel via {@link #buildPanel(EventResolutionDto)}.</li>
     *   <li>Creates a full-screen backdrop, wires a click handler to {@link #advance()},
     *       adds it to the scene root, and plays the fade-in animation.</li>
     *   <li>On fade-in completion, calls {@link #startDwell()} to begin the timer.</li>
     * </ol>
     * </p>
     */
    private static void showNext() {
        PendingEvent next = queue.poll();
        if (next == null) {
            activeRoot = null;
            if (onQueueDrained != null) {
                Runnable cb = onQueueDrained;
                onQueueDrained = null;
                cb.run();
            }
            return;
        }

        TribePopupController.closeIfOpen();

        StackPane root = next.root();
        EventResolutionDto dto = next.dto();

        StackPane backdrop = new StackPane();
        backdrop.getStyleClass().add("mesos-event-backdrop");
        backdrop.setOpacity(0);
        backdrop.setOnMouseClicked(e -> advance());
        backdrop.setPickOnBounds(true);

        StackPane slab = buildPanel(dto);
        backdrop.getChildren().add(slab);

        root.getChildren().add(backdrop);
        activeBackdrop = backdrop;
        activeRoot = root;

        FadeTransition in = new FadeTransition(Duration.millis(FADE_IN_MS), backdrop);
        in.setFromValue(0);
        in.setToValue(1);
        in.setOnFinished(ev -> startDwell());
        in.play();
    }

    /**
     * Starts the countdown after the fade-in animation completes.
     * Resumes {@link #activeProgress} (the bar animation) and creates a
     * {@link PauseTransition} for {@link #DWELL_SECONDS} seconds that calls
     * {@link #advance()} when it expires.
     */
    private static void startDwell() {
        if (activeProgress != null) activeProgress.play();

        PauseTransition dwell = new PauseTransition(Duration.seconds(DWELL_SECONDS));
        dwell.setOnFinished(e -> advance());
        activeDwell = dwell;
        dwell.play();
    }

    //Panel builder -----------------------------------------------------

    /**
     * Preferred width in pixels of the stone-slab background image.
     * The background {@link ImageView} is fitted to this width with ratio preserved.
     */
    private static final double SLAB_W = 1240;

    /**
     * Maximum width in pixels of the text-content area carved visually into
     * the centre of the stone slab.
     */
    private static final double CONTENT_W = 460;

    /**
     * Builds the full visual panel (stone-slab background + content overlay) for
     * a single event. Also creates and assigns {@link #activeProgress}, the
     * {@link Timeline} that animates the countdown {@link ProgressBar}.
     * <p>
     * The panel layout from top to bottom:
     * <ol>
     *   <li>Header row — event-type icon, title, and era label ({@link #buildHeader}).</li>
     *   <li>Horizontal separator.</li>
     *   <li>One {@link #buildPlayerRow} per entry in {@code dto.deltas}.</li>
     *   <li>6 px gap.</li>
     *   <li>Countdown {@link ProgressBar} (4 px tall, drains left-to-right).</li>
     *   <li>Footer with "tap to continue" hint.</li>
     * </ol>
     * </p>
     *
     * @param dto the event data used to populate the panel
     * @return the fully constructed slab {@link StackPane} ready to be added to a backdrop
     */
    private static StackPane buildPanel(EventResolutionDto dto) {
        StackPane slab = new StackPane();
        slab.getStyleClass().add("mesos-event-slab");
        slab.setMaxWidth(Region.USE_PREF_SIZE);
        slab.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(slab, Pos.CENTER);

        Image bgImg = ImageCache.get("/images/wallpapers/EventOverlayBackground.png");
        if (bgImg != null) {
            ImageView bgView = new ImageView(bgImg);
            bgView.setFitWidth(SLAB_W);
            bgView.setPreserveRatio(true);
            bgView.setSmooth(true);
            slab.getChildren().add(bgView);
        }

        VBox content = new VBox(0);
        content.setMaxWidth(CONTENT_W);
        content.setMinWidth(340);
        content.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(content, Pos.CENTER);

        content.getChildren().add(buildHeader(dto));

        Region sep = new Region();
        sep.getStyleClass().add("mesos-event-sep");
        sep.setMinHeight(1);
        sep.setMaxHeight(1);
        content.getChildren().add(sep);

        for (PlayerEventDeltaDto d : dto.deltas) {
            content.getChildren().add(buildPlayerRow(d));
        }

        Region gap = new Region();
        gap.setMinHeight(6);
        content.getChildren().add(gap);

        ProgressBar bar = new ProgressBar(1.0);
        bar.getStyleClass().add("mesos-event-progress");
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setMinHeight(4);
        bar.setMaxHeight(4);
        content.getChildren().add(bar);

        Label hint = new Label("tap to continue");
        hint.getStyleClass().add("mesos-event-tap-hint");
        HBox footer = new HBox(hint);
        footer.getStyleClass().add("mesos-event-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);
        content.getChildren().add(footer);

        slab.getChildren().add(content);

        // Assigned to activeProgress and started in startDwell() after fade-in
        activeProgress = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(bar.progressProperty(), 1.0)),
                new KeyFrame(Duration.seconds(DWELL_SECONDS),
                        new KeyValue(bar.progressProperty(), 0.0, Interpolator.LINEAR))
        );

        return slab;
    }

    /**
     * Builds the header row for an event panel: a type-specific icon on the left
     * and a {@link VBox} with the human-readable event name and era label on the right.
     *
     * @param dto the event data used to resolve the icon and display strings
     * @return the constructed header {@link HBox}
     */
    private static HBox buildHeader(EventResolutionDto dto) {
        HBox header = new HBox(12);
        header.getStyleClass().add("mesos-event-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Image icon = ImageCache.get("/images/icons/" + iconForEvent(dto.eventType));
        if (icon != null) {
            ImageView iv = new ImageView(icon);
            iv.setFitWidth(30);
            iv.setFitHeight(30);
            iv.setPreserveRatio(true);
            header.getChildren().add(iv);
        }

        VBox titleBox = new VBox(3);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label title = new Label(prettyEventType(dto.eventType));
        title.getStyleClass().add("mesos-event-title");

        Label eraLabel = new Label(prettyEra(dto.era));
        eraLabel.getStyleClass().add("mesos-event-era");

        titleBox.getChildren().addAll(title, eraLabel);
        header.getChildren().add(titleBox);

        return header;
    }

    /**
     * Builds a single player row showing the player's name alongside their
     * food and prestige stat changes. An optional detail label is appended below
     * the stat row when {@link PlayerEventDeltaDto#details} is non-blank.
     *
     * @param d the per-player delta data for this event
     * @return the constructed row {@link VBox}
     */
    private static VBox buildPlayerRow(PlayerEventDeltaDto d) {
        VBox container = new VBox(2);
        container.getStyleClass().add("mesos-event-player-row");

        HBox mainRow = new HBox(16);
        mainRow.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(d.playerName);
        name.getStyleClass().add("mesos-event-player-name");
        name.setMinWidth(100);
        name.setMaxWidth(100);

        mainRow.getChildren().addAll(
                name,
                buildStatBox("/images/icons/food.png", d.foodBefore, d.foodAfter),
                buildStatBox("/images/icons/PuntiPrestige.png", d.prestigeBefore, d.prestigeAfter)
        );
        container.getChildren().add(mainRow);

        if (d.details != null && !d.details.isBlank()) {
            Label details = new Label(d.details);
            details.getStyleClass().add("mesos-event-details");
            details.setWrapText(true);
            container.getChildren().add(details);
        }

        return container;
    }

    /**
     * Builds a small icon + value + delta {@link HBox} for a single stat (food or prestige).
     * The delta label is styled green for positive changes, red for negative, and
     * neutral for no change.
     *
     * @param iconPath classpath path to the stat icon image
     * @param before the stat value before the event was applied
     * @param after the stat value after the event was applied
     * @return the constructed stat {@link HBox}
     */
    private static HBox buildStatBox(String iconPath, int before, int after) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);

        Image img = ImageCache.get(iconPath);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(16);
            iv.setFitHeight(16);
            iv.setPreserveRatio(true);
            box.getChildren().add(iv);
        }

        int delta = after - before;
        Label value = new Label(String.valueOf(after));
        value.getStyleClass().add("mesos-event-stat-value");

        String deltaText;
        String deltaStyle;
        if (delta > 0) {
            deltaText = "(+" + delta + ")"; deltaStyle = "mesos-event-delta-pos";
        } else if (delta < 0) {
            deltaText = "("  + delta + ")"; deltaStyle = "mesos-event-delta-neg";
        } else {
            deltaText = "(—)"; deltaStyle = "mesos-event-delta-zero";
        }

        Label dl = new Label(deltaText);
        dl.getStyleClass().add(deltaStyle);

        box.getChildren().addAll(value, dl);
        return box;
    }

    // Helpers ----------------------------------------------------------

    /**
     * Maps an event-type string to the filename of its icon inside
     * {@code /images/icons/}. Returns {@code "star.png"} for unknown types.
     *
     * @param type the raw event-type identifier (e.g. {@code "HUNT"})
     * @return the icon filename, never {@code null}
     */
    private static String iconForEvent(String type) {
        if (type == null) return "star.png";
        return switch (type) {
            case "HUNT" -> "Hunter.png";
            case "CAVE_PAINTINGS"  -> "Artist.png";
            case "SHAMANIC_RITUAL" -> "Shaman.png";
            case "SUSTENANCE" -> "Gatherer.png";
            case "NASCONDINO" -> "Inventor.png";
            default -> "star.png";
        };
    }

    /**
     * Converts a raw event-type identifier to a readable display string.
     * Returns the raw value unchanged for unrecognised types.
     *
     * @param type the raw event-type identifier (e.g. {@code "CAVE_PAINTINGS"})
     * @return the display string (e.g. {@code "Cave Paintings"})
     */
    private static String prettyEventType(String type) {
        if (type == null) return "Event";
        return switch (type) {
            case "HUNT" -> "Hunt";
            case "CAVE_PAINTINGS"  -> "Cave Paintings";
            case "SHAMANIC_RITUAL" -> "Shamanic Ritual";
            case "SUSTENANCE" -> "Sustenance";
            case "NASCONDINO" -> "Nascondino";
            default -> type;
        };
    }

    /**
     * Converts a raw era identifier to a readable display string.
     * Returns the raw value unchanged for unrecognised identifiers.
     *
     * @param era the raw era identifier (e.g. {@code "ERA_II"})
     * @return the display string (e.g. {@code "Era II"}), or an empty string if {@code null}
     */
    private static String prettyEra(String era) {
        if (era == null) return "";
        return switch (era) {
            case "ERA_I" -> "Era I";
            case "ERA_II" -> "Era II";
            case "ERA_III" -> "Era III";
            default -> era;
        };
    }
}
