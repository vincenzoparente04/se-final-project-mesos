package view.board;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import network.client.core.LocalGameState;
import shared.dto.CardDto;
import shared.dto.PlayerDto;
import view.PlayerViewController;
import view.SceneRouter;
import view.ViewController;
import view.widgets.CardView;
import view.widgets.CardZoomOverlay;
import view.widgets.ImageCache;
import view.widgets.SummaryCardOverlay;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Sub-controller for the self-player bar at the bottom of the board:
 * totem marker, tribe columns, building fan, and colour picker.
 * Initialised by {@link BoardViewController#bind} after FXML injection.
 */
public class BoardSelfPanelController implements ViewController {

    private static final double SELF_CARD_W = 90;
    private static final double SELF_CARD_H = 130;

    @FXML private HBox     rootBar;
    @FXML private StackPane selfPlayerSlot;
    @FXML private HBox selfTribeSlot;
    @FXML private HBox selfBuildingsSlot;

    public double panelHeight() {
        double h = rootBar.getHeight();
        return h > 0 ? h : rootBar.getPrefHeight();
    }

    private SceneRouter router;
    private StackPane overlayRoot;

    public void init(SceneRouter router, StackPane overlayRoot) {
        this.router = router;
        this.overlayRoot = overlayRoot;
        attachSummaryCard();
    }

    /** Appends a spacer + SummaryCard thumbnail anchored to the far right of the self panel. */
    private void attachSummaryCard() {
        Image img = ImageCache.get("/images/SummaryCard.png");
        if (img == null) return;

        ImageView thumb = new ImageView(img);
        thumb.setFitHeight(SELF_CARD_H);
        thumb.setPreserveRatio(true);
        thumb.setStyle("-fx-cursor: hand;");
        thumb.setOnMouseClicked(e -> SummaryCardOverlay.show(overlayRoot, img));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        rootBar.getChildren().addAll(spacer, thumb);
    }

    @Override
    public void update(LocalGameState state) {
        String phase = state.getPhase();
        String me = router.playerName();
        boolean isMyTurn = me != null && me.equals(state.getCurrentPlayerName());

        updateSelfPanel(state.getPlayers(), me, state.getCurrentPlayerName());
    }

    // Self panel ---------------------------------------------------------------

    private void updateSelfPanel(List<PlayerDto> players, String me, String currentPlayer) {
        PlayerDto self = players.stream().filter(p -> p.name.equals(me)).findFirst().orElse(null);
        if (self == null) {
            selfPlayerSlot.getChildren().clear();
            selfTribeSlot.getChildren().clear();
            selfBuildingsSlot.getChildren().clear();
            return;
        }
        boolean isCurrent = self.name.equals(currentPlayer);
        PlayerViewController pvc = PlayerViewController.load(self, true, isCurrent);
        selfPlayerSlot.getChildren().setAll(pvc.root());

        List<CardDto> chars = self.tribe != null && self.tribe.characterCards != null
                ? self.tribe.characterCards : List.of();
        List<CardDto> builds = self.tribe != null && self.tribe.buildings != null
                ? self.tribe.buildings : List.of();

        selfTribeSlot.getChildren().setAll(buildTribeGroups(chars));
        // Buildings use a hidden header placeholder to top-align with character columns.
        selfBuildingsSlot.getChildren().setAll(buildHeaderlessGroup(builds));
    }

    private VBox buildHeaderlessGroup(List<CardDto> cards) {
        VBox group = new VBox(4);
        group.setAlignment(Pos.TOP_CENTER);

        // Invisible placeholder keeps the cards row at the same vertical offset
        // as the section headers used for character groups.
        Label spacer = new Label(" ");
        spacer.getStyleClass().add("mesos-section-label");
        spacer.setVisible(false);

        HBox stack = new HBox(-58);
        stack.setAlignment(Pos.CENTER_LEFT);
        for (CardDto c : cards) {
            CardView v = new CardView(c, true, SELF_CARD_W, SELF_CARD_H);
            v.setStyle("-fx-cursor: hand;");
            v.setOnContextMenuRequested(e -> CardZoomOverlay.show(overlayRoot, c));
            stack.getChildren().add(v);
        }
        group.getChildren().addAll(spacer, stack);
        return group;
    }

    private List<VBox> buildTribeGroups(List<CardDto> cards) {
        Map<String, List<CardDto>> grouped = new LinkedHashMap<>();
        for (String t : new String[]{"HUNTER", "BUILDER", "SHAMAN", "ARTIST", "INVENTOR", "GATHERER"}) {
            grouped.put(t, new java.util.ArrayList<>());
        }
        for (CardDto c : cards) grouped.computeIfAbsent(c.type, k -> new java.util.ArrayList<>()).add(c);

        List<VBox> out = new java.util.ArrayList<>();
        for (Map.Entry<String, List<CardDto>> e : grouped.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            out.add(buildSingleGroup(BoardGameAreaController.prettyType(e.getKey()), e.getValue()));
        }
        return out;
    }

    private VBox buildSingleGroup(String title, List<CardDto> cards) {
        VBox group = new VBox(4);
        group.setAlignment(Pos.TOP_CENTER);

        Label header = new Label(title + " ×" + cards.size());
        header.getStyleClass().add("mesos-section-label");

        HBox stack = new HBox(-58);
        stack.setAlignment(Pos.CENTER_LEFT);
        for (CardDto c : cards) {
            CardView v = new CardView(c, true, SELF_CARD_W, SELF_CARD_H);
            v.setStyle("-fx-cursor: hand;");
            v.setOnContextMenuRequested(e -> CardZoomOverlay.show(overlayRoot, c));
            stack.getChildren().add(v);
        }
        group.getChildren().addAll(header, stack);
        return group;
    }



}
