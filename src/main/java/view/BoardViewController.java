package view;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import network.client.LocalGameState;
import shared.dto.CardDto;
import shared.dto.OfferTileDto;
import shared.dto.PlayerDto;
import view.widgets.CardView;
import view.widgets.CardZoomOverlay;
import view.widgets.DeckView;
import view.widgets.OfferTileView;
import view.widgets.TurnOrderTileView;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Main in-game scene. Receives full {@link LocalGameState} snapshots from the listener
 * and rebuilds every panel from scratch — the DTO is small enough that this is simpler
 * than diffing, and JavaFX handles the re-layout fine.
 */
public class BoardViewController {

    // Card sizes: the bottom row enlarges during end-of-round so the events being
    // resolved are obvious; everything else stays compact.
    private static final double CARD_W_NORMAL   = 84;
    private static final double CARD_H_NORMAL   = 122;
    private static final double CARD_W_RESOLVED = 120;
    private static final double CARD_H_RESOLVED = 174;
    private static final double SELF_CARD_W     = 90;
    private static final double SELF_CARD_H     = 130;

    @FXML private StackPane rootPane;
    @FXML private Label phaseLabel;
    @FXML private Label roundLabel;
    @FXML private Label eraLabel;
    @FXML private Label currentPlayerLabel;
    @FXML private Button endTurnButton;

    @FXML private HBox othersBar;
    @FXML private HBox upperRowsBox;
    @FXML private VBox centralBox;
    @FXML private HBox lowerRowsBox;
    @FXML private VBox decksBox;

    @FXML private StackPane selfPlayerSlot;
    @FXML private HBox selfTribeSlot;
    @FXML private HBox selfBuildingsSlot;
    @FXML private HBox colorPicker;

    private SceneRouter router;
    private boolean winnerShown = false;

    public void bind(SceneRouter router) {
        this.router = router;
        LocalGameState state = router.localState();
        if (state != null && state.snapshot() != null) update(state);
    }

    public StackPane root() { return rootPane; }

    // ── Main update ────────────────────────────────────────────────────────

    public void update(LocalGameState state) {
        String phase = state.getPhase();
        String me = router.playerName();
        boolean isMyTurn = me != null && me.equals(state.getCurrentPlayerName());

        // TODO: banner "evento risolto" — richiede aggiunta EventResolvedMessage al protocollo, posticipato

        Map<String, PlayerDto> playersByName = indexByName(state.getPlayers());

        updateStatusBar(state, phase, isMyTurn);
        updateColorPicker(phase, isMyTurn);

        updatePlayersBar(state.getPlayers(), me, state.getCurrentPlayerName());
        updateSelfPanel(state.getPlayers(), me, state.getCurrentPlayerName());

        updateCentralBox(state, playersByName, phase, isMyTurn);
        boolean resolving = isEndOfRoundPhase(phase);
        updateRowsBox(upperRowsBox, state.getTopRowTribe(), state.getTopRowBuilding(),
                phase, isMyTurn, false);
        updateRowsBox(lowerRowsBox, state.getBottomRowTribe(), state.getBottomRowBuilding(),
                phase, isMyTurn, resolving);
        updateDecks(state.getCurrentEra());

        if (state.isGameOver() && !winnerShown) {
            winnerShown = true;
            router.toWinner(state.getPlayers(), state.getWinners());
        }
    }

    // ── Status bar ─────────────────────────────────────────────────────────

    private void updateStatusBar(LocalGameState state, String phase, boolean isMyTurn) {
        phaseLabel.setText("Phase: " + formatPhase(phase));
        roundLabel.setText("Round " + state.getCurrentRound());
        eraLabel.setText(state.getCurrentEra() != null ? "Era " + state.getCurrentEra() : "");
        String cp = state.getCurrentPlayerName();
        currentPlayerLabel.setText(cp != null ? cp + (isMyTurn ? " (you)" : "") + "'s turn" : "—");

        boolean showEnd = "ACTION".equals(phase) && isMyTurn;
        endTurnButton.setVisible(showEnd);
        endTurnButton.setManaged(showEnd);
    }

    // ── Color picker (sits in the bottom self-bar) ─────────────────────────

    private void updateColorPicker(String phase, boolean isMyTurn) {
        boolean show = "COLOR_CHOOSING_PHASE".equals(phase) && isMyTurn;
        colorPicker.setVisible(show);
        colorPicker.setManaged(show);
        if (!show) return;
        if (!colorPicker.getChildren().isEmpty()) return;

        String[] colors = {"RED", "BLUE", "GREEN", "YELLOW", "WHITE"};
        for (String c : colors) {
            Button btn = new Button();
            btn.getStyleClass().addAll("mesos-totem-btn", "mesos-totem-" + c);
            btn.setOnAction(e -> {
                if (router.virtualServer() != null) router.virtualServer().sendChooseColor(c);
            });
            colorPicker.getChildren().add(btn);
        }
    }

    // ── Other players (above the play area) ────────────────────────────────

    private void updatePlayersBar(List<PlayerDto> players, String me, String currentPlayer) {
        othersBar.getChildren().clear();
        for (PlayerDto p : players) {
            if (p.name.equals(me)) continue;
            boolean isCurrent = p.name.equals(currentPlayer);
            PlayerViewController pvc = PlayerViewController.load(p, false, isCurrent);
            Parent node = pvc.root();
            node.setOnMouseClicked(e -> TribePopupController.show(rootPane.getScene().getWindow(), p));
            othersBar.getChildren().add(node);
        }
    }

    // ── Self panel ─────────────────────────────────────────────────────────

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
        selfBuildingsSlot.getChildren().setAll(buildBuildingsStack(builds));
    }

    private HBox buildBuildingsStack(List<CardDto> buildings) {
        HBox stack = new HBox(-58);
        stack.setAlignment(Pos.CENTER_LEFT);
        for (CardDto c : buildings) {
            CardView v = new CardView(c, true, SELF_CARD_W, SELF_CARD_H);
            v.setStyle("-fx-cursor: hand;");
            v.setOnMouseClicked(e -> CardZoomOverlay.show(rootPane, c));
            stack.getChildren().add(v);
        }
        return stack;
    }

    /**
     * Group character cards by sub-type ("HUNTER", "BUILDER", …) so the player
     * can read their own tribe at a glance. Each group is a column with a header
     * and the cards fanned vertically with a generous overlap.
     */
    private List<VBox> buildTribeGroups(List<CardDto> cards) {
        Map<String, java.util.List<CardDto>> grouped = new LinkedHashMap<>();
        // Stable group order for the UI
        for (String t : new String[]{"HUNTER", "BUILDER", "SHAMAN", "ARTIST", "INVENTOR", "GATHERER"}) {
            grouped.put(t, new java.util.ArrayList<>());
        }
        for (CardDto c : cards) grouped.computeIfAbsent(c.type, k -> new java.util.ArrayList<>()).add(c);

        java.util.List<VBox> out = new java.util.ArrayList<>();
        for (Map.Entry<String, java.util.List<CardDto>> e : grouped.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            out.add(buildSingleGroup(prettyType(e.getKey()), e.getValue()));
        }
        return out;
    }

    private VBox buildSingleGroup(String title, List<CardDto> cards) {
        VBox group = new VBox(4);
        group.setAlignment(Pos.TOP_CENTER);

        Label header = new Label(title + " ×" + cards.size());
        header.getStyleClass().add("mesos-section-label");

        HBox stack = new HBox(-58);  // generous overlap — fanned hand of cards
        stack.setAlignment(Pos.CENTER_LEFT);
        for (CardDto c : cards) {
            CardView v = new CardView(c, true, SELF_CARD_W, SELF_CARD_H);
            v.setStyle("-fx-cursor: hand;");
            v.setOnMouseClicked(e -> CardZoomOverlay.show(rootPane, c));
            stack.getChildren().add(v);
        }
        group.getChildren().addAll(header, stack);
        return group;
    }

    // ── Central area: TurnOrderTile + OfferTrack ───────────────────────────

    private void updateCentralBox(LocalGameState state,
                                  Map<String, PlayerDto> playersByName,
                                  String phase, boolean isMyTurn) {
        centralBox.getChildren().clear();

        TurnOrderTileView turnOrder = new TurnOrderTileView(state.getTurnOrderSlots(), playersByName);
        centralBox.getChildren().add(turnOrder);

        boolean canPlace = "PLACEMENT".equals(phase) && isMyTurn;
        HBox offerTrack = new HBox(8);
        offerTrack.setAlignment(Pos.CENTER);
        for (OfferTileDto tile : state.getOfferTiles()) {
            offerTrack.getChildren().add(new OfferTileView(
                    tile, playersByName, canPlace,
                    letter -> {
                        if (router.virtualServer() != null) {
                            router.virtualServer().sendPlaceTotem(letter);
                        }
                    }));
        }
        centralBox.getChildren().add(offerTrack);
    }

    // ── Tribe + Building rows on the board ─────────────────────────────────

    private void updateRowsBox(HBox box,
                               List<CardDto> tribeCards,
                               List<CardDto> buildingCards,
                               String phase, boolean isMyTurn,
                               boolean resolving) {
        box.getChildren().clear();
        box.setAlignment(Pos.CENTER);

        double w = resolving ? CARD_W_RESOLVED : CARD_W_NORMAL;
        double h = resolving ? CARD_H_RESOLVED : CARD_H_NORMAL;

        box.getChildren().add(buildRow(tribeCards,    phase, isMyTurn, w, h));
        box.getChildren().add(buildRow(buildingCards, phase, isMyTurn, w, h));
    }

    private HBox buildRow(List<CardDto> cards, String phase, boolean isMyTurn,
                          double cardW, double cardH) {
        boolean canDraw = "ACTION".equals(phase) && isMyTurn;
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER);
        for (CardDto c : cards) {
            CardView v = new CardView(c, true, cardW, cardH);
            v.setStyle("-fx-cursor: hand;");
            if (canDraw) {
                v.setOnMouseClicked(e -> {
                    if (router.virtualServer() != null) router.virtualServer().sendDrawCard(c.id);
                });
            } else {
                v.setOnMouseClicked(e -> CardZoomOverlay.show(rootPane, c));
            }
            row.getChildren().add(v);
        }
        return row;
    }

    // ── Decks on the right ─────────────────────────────────────────────────

    private void updateDecks(String era) {
        decksBox.getChildren().clear();
        int eraNum = eraNumber(era);

        Label tribeLbl = new Label("Tribe deck");
        tribeLbl.getStyleClass().add("mesos-section-label");
        Label buildLbl = new Label("Building deck");
        buildLbl.getStyleClass().add("mesos-section-label");

        DeckView tribeDeck = new DeckView("BackEra" + eraNum + ".png");
        DeckView buildingDeck = new DeckView(buildingBackForEra(eraNum));

        VBox td = new VBox(4, tribeLbl, tribeDeck);
        td.setAlignment(Pos.CENTER);
        VBox bd = new VBox(4, buildLbl, buildingDeck);
        bd.setAlignment(Pos.CENTER);
        decksBox.getChildren().addAll(td, bd);
    }

    /** Mirrors the model's filename: era 1 & 2 use "BackBuildinaEra" (typo in asset), era 3 uses "BackBuildingEra". */
    private String buildingBackForEra(int eraNum) {
        return switch (eraNum) {
            case 1 -> "BackBuildinaEra1.png";
            case 2 -> "BackBuildinaEra2.png";
            default -> "BackBuildingEra3.png";
        };
    }

    private int eraNumber(String era) {
        if (era == null) return 1;
        return switch (era) {
            case "ERA_I", "I", "1"    -> 1;
            case "ERA_II", "II", "2"  -> 2;
            case "ERA_III", "III", "3" -> 3;
            default -> 1;
        };
    }

    private boolean isEndOfRoundPhase(String phase) {
        return "END_OF_ROUND".equals(phase) || "PRE_END_OF_ROUND".equals(phase);
    }

    // ── Handlers / formatting ──────────────────────────────────────────────

    @FXML
    private void onEndTurn() {
        if (router.virtualServer() != null) router.virtualServer().sendEndTurn();
    }

    private static Map<String, PlayerDto> indexByName(List<PlayerDto> players) {
        Map<String, PlayerDto> m = new HashMap<>();
        for (PlayerDto p : players) m.put(p.name, p);
        return m;
    }

    private static String prettyType(String type) {
        if (type == null) return "?";
        return switch (type) {
            case "HUNTER"   -> "Hunters";
            case "BUILDER"  -> "Builders";
            case "SHAMAN"   -> "Shamans";
            case "ARTIST"   -> "Artists";
            case "INVENTOR" -> "Inventors";
            case "GATHERER" -> "Gatherers";
            default -> type;
        };
    }

    private static String formatPhase(String phase) {
        if (phase == null) return "—";
        return switch (phase) {
            case "SETUP"                -> "Setup";
            case "COLOR_CHOOSING_PHASE" -> "Choose color";
            case "PLACEMENT"            -> "Placement";
            case "ACTION"               -> "Action";
            case "PRE_END_OF_ROUND"     -> "Resolving…";
            case "END_OF_ROUND"         -> "End of round";
            case "END_OF_GAME"          -> "End of game";
            default                     -> phase;
        };
    }
}
