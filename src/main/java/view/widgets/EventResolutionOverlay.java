package view.widgets;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import shared.dto.event.EventResolutionDto;
import shared.dto.event.PlayerEventDeltaDto;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Themed modal overlay that presents event-resolution results one at a time.
 * Multiple events are queued: each is shown for {@code DWELL_SECONDS} seconds
 * with a visible countdown bar, then the next is shown automatically.
 * Clicking anywhere advances to the next event immediately.
 */
public final class EventResolutionOverlay {

    private static final double DWELL_SECONDS  = 12.0;
    private static final double FADE_IN_MS     = 300;
    private static final double FADE_OUT_MS    = 200;

    private record PendingEvent(StackPane root, EventResolutionDto dto) {}

    private static final Deque<PendingEvent> queue = new ArrayDeque<>();
    private static StackPane         activeRoot    = null;
    private static StackPane         activeBackdrop = null;
    private static PauseTransition   activeDwell   = null;
    private static Timeline          activeProgress = null;

    private EventResolutionOverlay() {}

    // Public API
    public static void enqueue(StackPane root, EventResolutionDto dto) {
        if (root == null || dto == null) return;
        queue.add(new PendingEvent(root, dto));
        if (activeBackdrop == null) showNext();
    }

    // Internal flow

    /** Cancels current display and moves to the next queued event (or clears). */
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

    private static void showNext() {
        PendingEvent next = queue.poll();
        if (next == null) { activeRoot = null; return; }

        StackPane root = next.root();
        EventResolutionDto dto = next.dto();
        int remaining = queue.size();   // how many more are still waiting

        StackPane backdrop = new StackPane();
        backdrop.getStyleClass().add("mesos-event-backdrop");
        backdrop.setOpacity(0);
        backdrop.setOnMouseClicked(e -> advance());
        backdrop.setPickOnBounds(true);

        StackPane slab = buildPanel(dto, remaining);
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

    private static void startDwell() {
        if (activeProgress != null) activeProgress.play();

        PauseTransition dwell = new PauseTransition(Duration.seconds(DWELL_SECONDS));
        dwell.setOnFinished(e -> advance());
        activeDwell = dwell;
        dwell.play();
    }

    //Panel builder -----------------------------------------------------

    private static final double SLAB_W = 1040;   // stone-slab image width
    private static final double CONTENT_W = 460;   // text area carved into the slab

    private static StackPane buildPanel(EventResolutionDto dto, int remaining) {
        //STONE SLAB
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

        // Content carved into the center of the slab
        VBox content = new VBox(0);
        content.setMaxWidth(CONTENT_W);
        content.setMinWidth(340);
        content.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(content, Pos.CENTER);

        content.getChildren().add(buildHeader(dto, remaining));

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

        // Progress timeline started in startDwell() after fade-in
        activeProgress = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(bar.progressProperty(), 1.0)),
                new KeyFrame(Duration.seconds(DWELL_SECONDS),
                        new KeyValue(bar.progressProperty(), 0.0, Interpolator.LINEAR))
        );

        return slab;
    }

    private static HBox buildHeader(EventResolutionDto dto, int remaining) {
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

        if (remaining > 0) {
            Label badge = new Label("+" + remaining + " more");
            badge.getStyleClass().add("mesos-event-badge");
            header.getChildren().add(badge);
        }

        return header;
    }

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
        if (delta > 0)      { deltaText = "(+" + delta + ")"; deltaStyle = "mesos-event-delta-pos"; }
        else if (delta < 0) { deltaText = "("  + delta + ")"; deltaStyle = "mesos-event-delta-neg"; }
        else                { deltaText = "(—)";               deltaStyle = "mesos-event-delta-zero"; }

        Label dl = new Label(deltaText);
        dl.getStyleClass().add(deltaStyle);

        box.getChildren().addAll(value, dl);
        return box;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String iconForEvent(String type) {
        if (type == null) return "star.png";
        return switch (type) {
            case "HUNT"            -> "Hunter.png";
            case "CAVE_PAINTINGS"  -> "Artist.png";
            case "SHAMANIC_RITUAL" -> "Shaman.png";
            case "SUSTENANCE"      -> "food.png";
            case "NASCONDINO"      -> "Inventor.png";
            default                -> "star.png";
        };
    }

    private static String prettyEventType(String type) {
        if (type == null) return "Event";
        return switch (type) {
            case "HUNT"            -> "Hunt";
            case "CAVE_PAINTINGS"  -> "Cave Paintings";
            case "SHAMANIC_RITUAL" -> "Shamanic Ritual";
            case "SUSTENANCE"      -> "Sustenance";
            case "NASCONDINO"      -> "Hide and Seek";
            default                -> type;
        };
    }

    private static String prettyEra(String era) {
        if (era == null) return "";
        return switch (era) {
            case "ERA_I"   -> "Era I";
            case "ERA_II"  -> "Era II";
            case "ERA_III" -> "Era III";
            default        -> era;
        };
    }
}
