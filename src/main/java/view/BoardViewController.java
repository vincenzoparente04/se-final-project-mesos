package view;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import network.client.LocalGameState;
import shared.dto.CardDto;
import shared.dto.OfferTileDto;
import shared.dto.PlayerDto;
import shared.dto.TurnOrderSlotDto;
import view.widgets.CardView;
import view.widgets.DeckView;
import view.widgets.OfferTileView;
import view.widgets.TurnOrderTileView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main in-game scene. Receives full {@link LocalGameState} snapshots from the listener
 * and rebuilds every panel from scratch — the DTO is small enough that this is simpler
 * than incremental diffs, and JavaFX handles the re-layout fine.
 */
public class BoardViewController {

    @FXML private StackPane rootPane;
    @FXML private Label phaseLabel;
    @FXML private Label roundLabel;
    @FXML private Label eraLabel;
    @FXML private Label currentPlayerLabel;

    @FXML private HBox colorPicker;
    @FXML private Button endTurnButton;

    @FXML private AnchorPane tablePane;
    @FXML private VBox boardBox;
    @FXML private HBox upperRowsBox;
    @FXML private VBox centralBox;
    @FXML private HBox lowerRowsBox;
    @FXML private VBox decksBox;

    @FXML private StackPane selfPlayerSlot;
    @FXML private StackPane selfTribeSlot;
    @FXML private StackPane selfBuildingsSlot;

    private SceneRouter router;
    private final HBox othersBar = new HBox(18);
    private boolean winnerShown = false;

    public void bind(SceneRouter router) {
        this.router = router;

        othersBar.setAlignment(Pos.CENTER);
        AnchorPane.setTopAnchor(othersBar, 14.0);
        AnchorPane.setLeftAnchor(othersBar, 0.0);
        AnchorPane.setRightAnchor(othersBar, 0.0);
        tablePane.getChildren().add(othersBar);

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

        updateStatusBar(state, phase, isMyTurn);
        updateColorPicker(phase, isMyTurn, state.getPlayers());

        Map<String, PlayerDto> playersByName = indexByName(state.getPlayers());

        updatePlayersBar(state.getPlayers(), me, state.getCurrentPlayerName());
        updateSelfPanel(state.getPlayers(), me, state.getCurrentPlayerName());

        updateCentralBox(state, playersByName, phase, isMyTurn);
        updateRowsBox(upperRowsBox, state.getTopRowTribe(), state.getTopRowBuilding(), phase, isMyTurn);
        updateRowsBox(lowerRowsBox, state.getBottomRowTribe(), state.getBottomRowBuilding(), phase, isMyTurn);
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
        currentPlayerLabel.setText(cp != null ? cp + (isMyTurn ? "  (you)" : "") + "'s turn" : "—");

        boolean showEnd = "ACTION".equals(phase) && isMyTurn;
        endTurnButton.setVisible(showEnd);
        endTurnButton.setManaged(showEnd);
    }

    // ── Color picker ───────────────────────────────────────────────────────

    private void updateColorPicker(String phase, boolean isMyTurn, List<PlayerDto> players) {
        boolean show = "COLOR_CHOOSING_PHASE".equals(phase) && isMyTurn;
        colorPicker.setVisible(show);
        colorPicker.setManaged(show);
        if (!show) return;
        if (!colorPicker.getChildren().isEmpty()) return;

        String[][] colors = {
                {"RED",    "#e74c3c"},
                {"BLUE",   "#3498db"},
                {"GREEN",  "#2ecc71"},
                {"YELLOW", "#f1c40f"},
                {"WHITE",  "#ecf0f1"}
        };
        for (String[] c : colors) {
            Button btn = new Button(c[0].substring(0, 1));
            btn.setStyle(
                    "-fx-background-color: " + c[1] + ";" +
                    "-fx-min-width: 32; -fx-min-height: 32; -fx-max-width: 32; -fx-max-height: 32;" +
                    "-fx-background-radius: 16;" +
                    "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;");
            btn.setOnAction(e -> {
                if (router.virtualServer() != null) router.virtualServer().sendChooseColor(c[0]);
            });
            colorPicker.getChildren().add(btn);
        }
    }

    // ── Players around the table ───────────────────────────────────────────

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

        selfTribeSlot.getChildren().setAll(buildFan(
                self.tribe != null ? self.tribe.characterCards : List.of()));
        selfBuildingsSlot.getChildren().setAll(buildFan(
                self.tribe != null ? self.tribe.buildings : List.of()));
    }

    private HBox buildFan(List<CardDto> cards) {
        // Negative spacing makes cards overlap like a fanned hand.
        HBox fan = new HBox(-50);
        for (CardDto c : cards) {
            fan.getChildren().add(new CardView(c, true, 80, 116));
        }
        fan.setPadding(new Insets(0, 0, 0, 0));
        return fan;
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

    // ── Tribe + Building rows ──────────────────────────────────────────────

    private void updateRowsBox(HBox box,
                               List<CardDto> tribeCards,
                               List<CardDto> buildingCards,
                               String phase, boolean isMyTurn) {
        box.getChildren().clear();
        box.setAlignment(Pos.CENTER);

        VBox tribeGroup = new VBox(4);
        tribeGroup.setAlignment(Pos.CENTER);
        Label tribeLabel = new Label("Tribe");
        tribeLabel.setStyle("-fx-text-fill: #aab7b8; -fx-font-size: 10; -fx-font-weight: bold;");
        tribeGroup.getChildren().add(tribeLabel);
        tribeGroup.getChildren().add(buildRow(tribeCards, phase, isMyTurn));

        VBox buildingGroup = new VBox(4);
        buildingGroup.setAlignment(Pos.CENTER);
        Label buildingLabel = new Label("Buildings");
        buildingLabel.setStyle("-fx-text-fill: #aab7b8; -fx-font-size: 10; -fx-font-weight: bold;");
        buildingGroup.getChildren().add(buildingLabel);
        buildingGroup.getChildren().add(buildRow(buildingCards, phase, isMyTurn));

        box.getChildren().addAll(tribeGroup, buildingGroup);
    }

    private HBox buildRow(List<CardDto> cards, String phase, boolean isMyTurn) {
        boolean canDraw = "ACTION".equals(phase) && isMyTurn;
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER);
        for (CardDto c : cards) {
            CardView v = new CardView(c, true, 70, 100);
            if (canDraw) {
                v.setStyle("-fx-cursor: hand;");
                v.setOnMouseClicked(e -> {
                    if (router.virtualServer() != null) router.virtualServer().sendDrawCard(c.id);
                });
            }
            row.getChildren().add(v);
        }
        return row;
    }

    // ── Decks on the right ─────────────────────────────────────────────────

    private void updateDecks(String era) {
        decksBox.getChildren().clear();
        int eraNum = eraNumber(era);
        DeckView tribeDeck = new DeckView("BackEra" + eraNum + ".png");
        DeckView buildingDeck = new DeckView(buildingBackForEra(eraNum));

        Label tribeLbl = new Label("Tribe deck");
        tribeLbl.setStyle("-fx-text-fill: #aab7b8; -fx-font-size: 10;");
        Label buildLbl = new Label("Building deck");
        buildLbl.setStyle("-fx-text-fill: #aab7b8; -fx-font-size: 10;");

        VBox td = new VBox(4, tribeLbl, tribeDeck);
        VBox bd = new VBox(4, buildLbl, buildingDeck);
        decksBox.getChildren().addAll(td, bd);
    }

    /** Mirrors the model's filename: era 1 & 2 use "BackBuildinaEra" (typo), era 3 uses "BackBuildingEra". */
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
            case "ERA_I", "I", "1"  -> 1;
            case "ERA_II", "II", "2" -> 2;
            case "ERA_III", "III", "3" -> 3;
            default -> 1;
        };
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

    private static String formatPhase(String phase) {
        if (phase == null) return "—";
        return switch (phase) {
            case "SETUP"                -> "Setup";
            case "COLOR_CHOOSING_PHASE" -> "Choose color";
            case "PLACEMENT"            -> "Placement";
            case "ACTION"               -> "Action";
            case "PRE_END_OF_ROUND"     -> "Pre-end of round";
            case "END_OF_ROUND"         -> "End of round";
            case "END_OF_GAME"          -> "End of game";
            default                     -> phase;
        };
    }
}
