package view.board;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import network.client.core.LocalGameState;
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

    private static final double CARD_ASPECT        = 122.0 / 84.0;
    private static final double RESOLVED_SCALE    = 120.0 / 84.0;

    // Proportion of one row's width used per card (row = half of upperRowsBox minus spacing)
    private static final double ROW_DIVISOR_SMALL = 4.5;   // ≤3 players → bigger cards
    private static final double ROW_DIVISOR_LARGE = 6.5;   // ≥4 players → smaller cards

    @FXML private HBox othersBar;
    @FXML private HBox upperRowsBox;
    @FXML private VBox centralBox;
    @FXML private HBox lowerRowsBox;
    @FXML private VBox decksBox;

    private SceneRouter router;
    private StackPane overlayRoot;
    private LocalGameState lastState;
    private Stage stage;
    private BoardSelfPanelController selfPanel;
    private double userScale = 0.88;

    public void init(SceneRouter router, StackPane overlayRoot, BoardSelfPanelController selfPanel) {
        this.router = router;
        this.overlayRoot = overlayRoot;
        this.stage = router.stage();
        this.selfPanel = selfPanel;

        rescale();
        rescaleCMD();
    }

    private double cardWidth(boolean resolving) {


        double stageW = stage != null ? stage.getWidth()  : 1280;
        double stageH = stage != null ? stage.getHeight() : 800;


        // Horizontal: subtract decksBox and padding to get the full row width,
        // then fit all n cards into that space
        double rowW = stageW - 220 - 70;
        int n = maxCardsInAnyRow();
        double wFromWidth = (rowW - 6.0 * (n - 1) - 40 ) / n;

        // Vertical: read actual heights from previous render,
        double othersH  = othersBar.getHeight()  > 0 ? othersBar.getHeight()  : 90;
        double centralH = centralBox.getHeight() > 0 ? centralBox.getHeight() : 120;
        double selfH    = selfPanel != null ? selfPanel.panelHeight() : 180;
        double availH   = Math.max(stageH - othersH - centralH - selfH - 90, 0);
        double wFromHeight = (availH / 2.0) / CARD_ASPECT;

        double base = Math.max(Math.min(wFromWidth, wFromHeight), 30);
        return resolving ? base * RESOLVED_SCALE * userScale : base * userScale;


    }

    private double cardHeight(boolean resolving) {
        return cardWidth(resolving) * CARD_ASPECT;
    }

    private double tileHeight() {
        return cardWidth(false) * (110.0 / 84.0);
    }

    /** Total cards in the fuller row (upper = topTribe+topBuilding, lower = bottomTribe+bottomBuilding). */
    private int maxCardsInAnyRow() {
        if (lastState == null) return 8;
        int upper = lastState.getTopRowTribe().size()    + lastState.getTopRowBuilding().size();
        int lower = lastState.getBottomRowTribe().size() + lastState.getBottomRowBuilding().size();
        return Math.max(upper, lower);
    }

    /**
     * Automatic rescaling of the gui dimensions.
     * Listens to stage width/height — those change only on user window resize,
     */
    private void rescale(){
        PauseTransition debounce = new PauseTransition(Duration.millis(100));
        debounce.setOnFinished(e -> { if (lastState != null) update(lastState); });

        stage.widthProperty().addListener((obs, old, w)  -> debounce.playFromStart());
        stage.heightProperty().addListener((obs, old, h) -> debounce.playFromStart());
    }

    /**
     *  Add CMD+/- to resize the gui dimensions
     */
    private void rescaleCMD(){
        overlayRoot.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                scene.setOnKeyPressed(e -> {
                    if (e.isMetaDown() || e.isControlDown() || e.isShiftDown()) {
                        if (e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.EQUALS) {
                            userScale = Math.min(userScale + 0.1, 2.0);
                            if (lastState != null) update(lastState);
                        } else if (e.getCode() == KeyCode.MINUS) {
                            userScale = Math.max(userScale - 0.1, 0.5);
                            if (lastState != null) update(lastState);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void update(LocalGameState state) {
        this.lastState = state;
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

    // Other players bar ---------------------------------------------------------------

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

    // Central area: TurnOrderTile + OfferTrack ---------------------------------------------------------------

    private void updateCentralBox(LocalGameState state,
                                  Map<String, PlayerDto> playersByName,
                                  String phase, boolean isMyTurn) {
        centralBox.getChildren().clear();

        double th = tileHeight();
        TurnOrderTileView turnOrder = new TurnOrderTileView(state.getTurnOrderSlots(), playersByName, th);

        boolean canPlace = "PLACEMENT".equals(phase) && isMyTurn;
        HBox offerTrack = new HBox(0);
        offerTrack.setAlignment(Pos.CENTER);
        for (OfferTileDto tile : state.getOfferTiles()) {
            offerTrack.getChildren().add(new OfferTileView(
                    tile, playersByName, canPlace,
                    letter -> router.getVirtualServer().sendPlaceTotem(letter), th));
        }

        HBox sideBySide = new HBox(5);
        sideBySide.setAlignment(Pos.CENTER);
        sideBySide.getChildren().addAll(turnOrder, offerTrack);
        centralBox.getChildren().add(sideBySide);
    }

    // Card rows ---------------------------------------------------------------

    private void updateRowsBox(HBox box,
                               List<CardDto> tribeCards,
                               List<CardDto> buildingCards,
                               String phase, boolean isMyTurn,
                               boolean resolving) {
        box.getChildren().clear();
        box.setAlignment(Pos.CENTER);

        double w = cardWidth(resolving);
        double h = cardHeight(resolving);

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
            v.getStyleClass().add("mesos-board-card");
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

    // Decks ---------------------------------------------------------------

    private void updateDecks(String era) {
        decksBox.getChildren().clear();
        int eraNum = eraNumber(era);
        int eraNumBuilding = eraNum + 1;

        double cw = cardWidth(false);
        double ch = cardHeight(false);

        //TODO: remove dead label code
        Label tribeLbl = new Label("");
        tribeLbl.getStyleClass().add("mesos-section-label");
        Label buildLbl = new Label("");
        buildLbl.getStyleClass().add("mesos-section-label");

        VBox td = new VBox(4, tribeLbl, new DeckView("BackEra" + eraNum + ".png", cw, ch));
        td.setAlignment(Pos.CENTER);
        if(eraNumBuilding < 4){
            VBox bd = new VBox(4, buildLbl, new DeckView(buildingBackForEra(eraNumBuilding), cw, ch));
            bd.setAlignment(Pos.CENTER);

            decksBox.getChildren().addAll(td, bd);
        }else{
            decksBox.getChildren().addAll(td);
        }

    }



    /** Mirrors the model's filename: 2 use "BackBuildinaEra" (typo in asset), era 3 uses "BackBuildingEra". */
    private String buildingBackForEra(int eraNum) {
        return switch (eraNum) {
            case 2 -> "BackBuildinaEra2.png";
            case 3 -> "BackBuildingEra3.png";
            default -> "";
        };
    }

    private int eraNumber(String era) {
        if (era == null) return 1;
        return switch (era) {
            case "ERA_I", "I", "1" -> 1;
            case "ERA_II", "II", "2" -> 2;
            case "ERA_III", "III", "3" -> 3;
            default -> 1;
        };
    }

    private boolean isEndOfRoundPhase(String phase) {
        return "END_OF_ROUND".equals(phase) || "PRE_END_OF_ROUND".equals(phase);
    }

    // Utilities ---------------------------------------------------------------

    private static Map<String, PlayerDto> indexByName(List<PlayerDto> players) {
        Map<String, PlayerDto> m = new HashMap<>();
        for (PlayerDto p : players) m.put(p.name, p);
        return m;
    }

    static String prettyType(String type) {
        if (type == null) return "?";
        return switch (type) {
            case "HUNTER" -> "Hunters";
            case "BUILDER" -> "Builders";
            case "SHAMAN" -> "Shamans";
            case "ARTIST" -> "Artists";
            case "INVENTOR" -> "Inventors";
            case "GATHERER" -> "Gatherers";
            default -> type;
        };
    }
}
