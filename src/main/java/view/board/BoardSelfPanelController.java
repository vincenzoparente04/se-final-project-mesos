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
import view.widgets.CardFormatter;
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

    /**Self card width*/
    private static final double SELF_CARD_W = 90;
    /**Self card height*/
    private static final double SELF_CARD_H = 130;

    /**Root HBox for the self bar*/
    @FXML private HBox rootBar;
    /**Totem slot on the left*/
    @FXML private StackPane selfPlayerSlot;
    /** HBox containing the tribe cards of the players*/
    @FXML private HBox selfTribeSlot;
    /** HBox containing the building cards of the players*/
    @FXML private HBox selfBuildingsSlot;

    /**
     * @return the panelHeight if positive, or else the preferred height of the panel.
     */
    public double panelHeight() {
        double h = rootBar.getHeight();
        return h > 0 ? h : rootBar.getPrefHeight();
    }

    /** Scene router to change between scenes*/
    private SceneRouter router;
    /**Overlay root*/
    private StackPane overlayRoot;

    /**Called by {@link BoardViewController}, initializes the self panel*/
    public void init(SceneRouter router, StackPane overlayRoot) {
        this.router = router;
        this.overlayRoot = overlayRoot;
        attachSummaryCard();
    }

    /** Appends a spacer and the SummaryCard thumbnail anchored to the far right of the self panel. */
    private void attachSummaryCard() {
        Image summary = ImageCache.get("/images/FrontCards/FrontCard22.png");
        Image sumBack = ImageCache.get("/images/BackCards/ExplanationCard.png");
        if (summary == null) return;

        // Build the image array
        Image[] slides =new Image[]{summary, sumBack};

        ImageView thumb = new ImageView(summary);
        thumb.setFitHeight(SELF_CARD_H);
        thumb.setPreserveRatio(true);
        thumb.setStyle("-fx-cursor: hand;");
        thumb.setOnMouseClicked(e -> SummaryCardOverlay.show(overlayRoot, slides));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        rootBar.getChildren().addAll(spacer, thumb);
    }

    /**
     * Called by the update() in {@link BoardViewController}.
     * It gets the self DTO and loads the playerViewController, then gets the cards of the player and renders them with two helper function.
     * @param state the game state given by the server
     */
    @Override
    public void update(LocalGameState state) {
        String me = router.playerName();

        //updateSelfPanel(state.getPlayers(), me, state.getCurrentPlayerName());

        List<PlayerDto> players = state.getPlayers();
        String currentPlayer = state.getCurrentPlayerName();

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
        selfBuildingsSlot.getChildren().setAll(buildHeaderlessGroup(builds));
    }


    /**
     * Called by {@link #update(LocalGameState)}, builds the building cards view. It uses an invisible placheolder to keep the cards aligned.
     * @param cards Dto of cards
     * @return a VBox containing the building cards
     */
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

    /**
     * Called by {@link #update(LocalGameState)}, builds the tribe cards view.
     * @param cards Dto of cards
     * @return A list of VBox, in which one element is a group of card based on the type of the card.
     */
    private List<VBox> buildTribeGroups(List<CardDto> cards) {
        Map<String, List<CardDto>> grouped = new LinkedHashMap<>();
        for (String t : new String[]{"HUNTER", "BUILDER", "SHAMAN", "ARTIST", "INVENTOR", "GATHERER"}) {
            grouped.put(t, new java.util.ArrayList<>());
        }
        for (CardDto c : cards) grouped.computeIfAbsent(c.type, k -> new java.util.ArrayList<>()).add(c);

        List<VBox> out = new java.util.ArrayList<>();
        for (Map.Entry<String, List<CardDto>> e : grouped.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            out.add(buildSingleGroup(CardFormatter.prettyType(e.getKey()), e.getValue()));
        }
        return out;
    }

    /**
     * Helper method to {@link #buildTribeGroups(List)}, it builds a VBox for one tribe, with the header and the cards.
     * @param title header of the group of cards, the type of cards
     * @param cards the cards to be rendered
     * @return the VBox containing the group of cards
     */
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
