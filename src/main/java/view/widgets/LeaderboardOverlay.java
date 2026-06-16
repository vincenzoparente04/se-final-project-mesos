package view.widgets;

import database.ScoreRecord;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Full-screen overlay showing the global DB leaderboard.
 * Click ✕ or the dark backdrop to dismiss.
 */
public final class LeaderboardOverlay {

    /** Formatter for date and time. */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /** Emoticons used as medals for the top 3 ranks. */
    private static final String[] MEDALS = { "🥇", "🥈", "🥉" };

    // Column widths  (total ≈ 644 + 28 left padding = fits 700px panel)
    /** Column width for rank. */
    private static final double W_RANK  = 56;
    /** Column width for nickname. */
    private static final double W_NICK  = 280;
    /** Column width for score. */
    private static final double W_SCORE = 120;
    /** Column width for date. */
    private static final double W_DATE  = 140;
    
    /**
    noooooooo
     */
    private LeaderboardOverlay() {}

    /**
     * Displays the leaderboard overlay on top of the given root pane.
     * 
     * @param root the root stack pane to attach the overlay to
     * @param leaderboard the list of score records to display
     * @param rankPosition the rank of the current player
     * @param myPoints the score of the current player
     * @param myName the nickname of the current player
     */
    public static void show(StackPane root, List<ScoreRecord> leaderboard,
                            int rankPosition, int myPoints, String myName) {

        StackPane backdrop = new StackPane();
        backdrop.getStyleClass().add("mesos-card-zoom-backdrop");

        VBox panel = new VBox(0);
        panel.getStyleClass().add("mesos-dialog-panel");
        panel.setMaxWidth(700);
        panel.setMaxHeight(740);

        // ── Header bar (title + close button) ──────────────────────────
        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(20, 16, 16, 28));

        Label title = new Label("Top 20 Leaderboard");
        title.getStyleClass().add("mesos-title-small");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("mesos-music-icon-btn");
        closeBtn.setPadding(new Insets(0, 4, 0, 4));
        closeBtn.setOnAction(e -> root.getChildren().remove(backdrop));

        titleBar.getChildren().addAll(title, spacer, closeBtn);

        // ── Subtitle (rank info) ────────────────────────────────────────
        Label subtitle = new Label("Your rank: #" + rankPosition + "   •   Your score: " + myPoints + " PP");
        subtitle.getStyleClass().add("mesos-label");
        subtitle.setOpacity(0.65);
        subtitle.setPadding(new Insets(0, 28, 18, 28));

        // ── Divider ─────────────────────────────────────────────────────
        Region divider = new Region();
        divider.setPrefHeight(1.5);
        divider.setStyle("-fx-background-color: rgba(232,104,42,0.35);");

        // ── Table ───────────────────────────────────────────────────────
        HBox header = buildHeader();
        header.setPadding(new Insets(10, 28, 10, 28));

        Region headerDivider = new Region();
        headerDivider.setPrefHeight(1);
        headerDivider.setStyle("-fx-background-color: rgba(232,104,42,0.2);");

        VBox rows = new VBox(0);
        for (int i = 0; i < leaderboard.size(); i++) {
            ScoreRecord r = leaderboard.get(i);
            boolean isMe = r.nickname().equals(myName);
            rows.getChildren().add(buildRow(i + 1, r, isMe));
        }

        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMaxHeight(540);
        scroll.setPrefHeight(540);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getChildren().addAll(titleBar, subtitle, divider, header, headerDivider, scroll);

        backdrop.getChildren().add(panel);
        backdrop.setOnMouseClicked(e -> {
            if (e.getTarget() == backdrop) root.getChildren().remove(backdrop);
        });

        root.getChildren().add(backdrop);
    }

    /**
     * Builds the header row for the leaderboard table.
     *
     * @return an HBox containing the header cells
     */
    private static HBox buildHeader() {
        HBox h = new HBox(0);
        h.setAlignment(Pos.CENTER_LEFT);

        Label rank  = headerCell("",        W_RANK);
        Label nick  = headerCell("PLAYER",  W_NICK);
        Label score = headerCell("SCORE",   W_SCORE);
        Label date  = headerCell("DATE",    W_DATE);
        score.setAlignment(Pos.CENTER_RIGHT);
        date.setAlignment(Pos.CENTER_RIGHT);

        h.getChildren().addAll(rank, nick, score, date);
        return h;
    }

    /**
     * Creates a header cell with the specified text and width.
     *
     * @param text the label text
     * @param w the preferred width of the label
     * @return a configured Label for the header cell
     */
    private static Label headerCell(String text, double w) {
        Label l = new Label(text);
        l.getStyleClass().add("mesos-winner-header-cell");
        l.setMinWidth(w);
        l.setPrefWidth(w);
        return l;
    }

    /**
     * Builds a single row for the leaderboard table.
     *
     * @param rank the rank position of the record
     * @param r the score record to display
     * @param isMe true if this record belongs to the current player, false otherwise
     * @return an HBox representing the table row
     */
    private static HBox buildRow(int rank, ScoreRecord r, boolean isMe) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 28, 10, 28));

        String baseStyle = "-fx-border-color: transparent transparent rgba(244,226,194,0.08) transparent; -fx-border-width: 0 0 1 0;";
        if (isMe) {
            row.setStyle(baseStyle + "-fx-background-color: rgba(232,104,42,0.14); -fx-background-radius: 0;");
        } else {
            row.setStyle(baseStyle);
        }

        // Rank: medal for top 3, number otherwise
        String rankText = rank <= 3 ? MEDALS[rank - 1] : "#" + rank;
        Label rankLbl = new Label(rankText);
        rankLbl.setMinWidth(W_RANK);
        rankLbl.setPrefWidth(W_RANK);
        rankLbl.setStyle("-fx-font-size: " + (rank <= 3 ? "18" : "13") + "; -fx-text-fill: rgba(184,158,115,0.9);");

        // Nickname
        Label nick = new Label(r.nickname() + (isMe ? "  ◀" : ""));
        nick.setMinWidth(W_NICK);
        nick.setPrefWidth(W_NICK);
        if (isMe) {
            nick.setStyle("-fx-text-fill: -mesos-ember-bright; -fx-font-weight: bold; -fx-font-size: 13;");
        } else {
            nick.setStyle("-fx-text-fill: -mesos-cream; -fx-font-size: 13;");
        }

        // Score
        Label score = new Label(r.score() + " PP");
        score.setMinWidth(W_SCORE);
        score.setPrefWidth(W_SCORE);
        score.setAlignment(Pos.CENTER_RIGHT);
        score.setStyle("-fx-text-fill: -mesos-ember-soft; -fx-font-weight: bold; -fx-font-size: 13;");

        // Date
        String dateStr = r.date() != null ? r.date().format(DATE_FMT) : "—";
        Label date = new Label(dateStr);
        date.setMinWidth(W_DATE);
        date.setPrefWidth(W_DATE);
        date.setAlignment(Pos.CENTER_RIGHT);
        date.setStyle("-fx-text-fill: rgba(184,158,115,0.55); -fx-font-size: 12;");

        row.getChildren().addAll(rankLbl, nick, score, date);
        return row;
    }
}
