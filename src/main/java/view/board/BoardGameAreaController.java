package view.board;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import network.client.LocalGameState;
import shared.dto.CardDto;
import shared.dto.OfferTileDto;
import shared.dto.PlayerDto;
import view.PlayerViewController;
import view.SceneRouter;
import view.TribePopupController;
import view.ViewController;
import view.widgets.CardView;
import view.widgets.CardZoomOverlay;
import view.widgets.DeckView;
import view.widgets.OfferTileView;
import view.widgets.TurnOrderTileView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sub-controller for the board's central play area:
 * opponents bar, card rows, offer track, turn order, and decks.
 * Initialised by {@link BoardViewController#bind} after FXML injection.
 */
public class BoardGameAreaController implements ViewController {

    private static final double CARD_W_NORMAL   = 84;
    private static final double CARD_H_NORMAL   = 122;
    private static final double CARD_W_RESOLVED = 120;
    private static final double CARD_H_RESOLVED = 174;

    @FXML private HBox othersBar;
    @FXML private HBox upperRowsBox;
    @FXML private VBox centralBox;
    @FXML private HBox lowerRowsBox;
    @FXML private VBox decksBox;

    private SceneRouter router;
    private StackPane overlayRoot;

    public void init(SceneRouter router, StackPane overlayRoot) {
        this.router = router;
        this.overlayRoot = overlayRoot;
    }

    @Override
    public void update(LocalGameState state) {
        String phase = state.getPhase();
        String me = router.playerName();
        boolean isMyTurn = me != null && me.equals(state.getCurrentPlayerName());
        Map<String, PlayerDto> playersByName = indexByName(state.getPlayers());

        updatePlayersBar(state.getPlayers(), me, state.getCurrentPlayerName());
        updateCentralBox(state, playersByName, phase, isMyTurn);
        boolean resolving = isEndOfRoundPhase(phase);
        updateRowsBox(upperRowsBox, state.getTopRowTribe(), state.getTopRowBuilding(), phase, isMyTurn, false);
        updateRowsBox(lowerRowsBox, state.getBottomRowTribe(), state.getBottomRowBuilding(), phase, isMyTurn, resolving);
        updateDecks(state.getCurrentEra());
    }

    // ── Other players bar ──────────────────────────────────────────────────

    private void updatePlayersBar(List<PlayerDto> players, String me, String currentPlayer) {
        othersBar.getChildren().clear();

        List<PlayerDto> opponents = players.stream()
                .filter(p -> !p.name.equals(me))
                .toList();
        if (opponents.isEmpty()) return;

        othersBar.getChildren().add(makeSpacer());
        for (PlayerDto p : opponents) {
            boolean isCurrent = p.name.equals(currentPlayer);
            PlayerViewController pvc = PlayerViewController.load(p, false, isCurrent);
            Parent node = pvc.root();
            node.setOnMouseClicked(e -> TribePopupController.show(overlayRoot.getScene().getWindow(), p));
            othersBar.getChildren().add(node);
            othersBar.getChildren().add(makeSpacer());
        }
    }

    private static Region makeSpacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    // ── Central area: TurnOrderTile + OfferTrack ───────────────────────────

    private void updateCentralBox(LocalGameState state,
                                  Map<String, PlayerDto> playersByName,
                                  String phase, boolean isMyTurn) {
        centralBox.getChildren().clear();

        TurnOrderTileView turnOrder = new TurnOrderTileView(state.getTurnOrderSlots(), playersByName);

        boolean canPlace = "PLACEMENT".equals(phase) && isMyTurn;
        HBox offerTrack = new HBox(0);
        offerTrack.setAlignment(Pos.CENTER);
        for (OfferTileDto tile : state.getOfferTiles()) {
            offerTrack.getChildren().add(new OfferTileView(
                    tile, playersByName, canPlace,
                    letter -> router.getVirtualServer().sendPlaceTotem(letter)));
        }

        HBox sideBySide = new HBox(5);
        sideBySide.setAlignment(Pos.CENTER);
        sideBySide.getChildren().addAll(turnOrder, offerTrack);
        centralBox.getChildren().add(sideBySide);
    }

    // ── Card rows ──────────────────────────────────────────────────────────

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
                v.setOnMouseClicked(e -> router.getVirtualServer().sendDrawCard(c.id));
            } else {
                v.setOnContextMenuRequested(e -> CardZoomOverlay.show(overlayRoot, c));
            }
            row.getChildren().add(v);
        }
        return row;
    }

    // ── Decks ──────────────────────────────────────────────────────────────

    private void updateDecks(String era) {
        decksBox.getChildren().clear();
        int eraNum = eraNumber(era);

        Label tribeLbl = new Label("Tribe deck");
        tribeLbl.getStyleClass().add("mesos-section-label");
        Label buildLbl = new Label("Building deck");
        buildLbl.getStyleClass().add("mesos-section-label");

        VBox td = new VBox(4, tribeLbl, new DeckView("BackEra" + eraNum + ".png"));
        td.setAlignment(Pos.CENTER);
        VBox bd = new VBox(4, buildLbl, new DeckView(buildingBackForEra(eraNum)));
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
            case "ERA_I",   "I",   "1" -> 1;
            case "ERA_II",  "II",  "2" -> 2;
            case "ERA_III", "III", "3" -> 3;
            default -> 1;
        };
    }

    private boolean isEndOfRoundPhase(String phase) {
        return "END_OF_ROUND".equals(phase) || "PRE_END_OF_ROUND".equals(phase);
    }

    // ── Utilities ──────────────────────────────────────────────────────────

    private static Map<String, PlayerDto> indexByName(List<PlayerDto> players) {
        Map<String, PlayerDto> m = new HashMap<>();
        for (PlayerDto p : players) m.put(p.name, p);
        return m;
    }

    static String prettyType(String type) {
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
}
