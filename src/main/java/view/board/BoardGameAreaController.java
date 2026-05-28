package view.board;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
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

    private static final double CARD_ASPECT      = 122.0 / 84.0;
    /** Fallback tile-height ratio used only before the first card-size computation. */
    private static final double TILE_H_RATIO    = 0.10;
    /** Fixed overhead: topBar(36) + inner-VBox padding(22) + inner-VBox spacing(28). */
    private static final double OVERHEAD_H      = 86.0;

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

    // Layout cache
    /** Cached base card width (without userScale). -1 means "needs recompute". */
    private double cachedCardW    = -1;
    /** Card count that was used for the last cachedCardW computation. */
    private int    cachedMaxCards = -1;
    /** Measured heights of stable panels; -1 until first successful measurement. */
    private double cachedOthersH  = -1;
    private double cachedCentralH = -1;
    /** True until the first post-layout re-measure has been scheduled. */
    private boolean needsInitialMeasure = true;

    public void init(SceneRouter router, StackPane overlayRoot, BoardSelfPanelController selfPanel) {
        this.router = router;
        this.overlayRoot = overlayRoot;
        this.stage = router.stage();
        this.selfPanel = selfPanel;

        rescale();
        rescaleCMD();
    }

    //  Scene-size helpers (prefer overlayRoot to avoid Windows decoration issues)

    private double effectiveW() {
        double w = (overlayRoot != null && overlayRoot.getWidth() > 1)
                ? overlayRoot.getWidth() : (stage != null ? stage.getWidth() : 1280);
        return w;
    }

    private double effectiveH() {
        double h = (overlayRoot != null && overlayRoot.getHeight() > 1)
                ? overlayRoot.getHeight() : (stage != null ? stage.getHeight() : 800);
        return h;
    }

    // ── Tile height: uses the CACHED card width → proportional to cards,
    //    but no circular dependency (cache is frozen during a render pass). ──

    private double tileHeight() {
        // cachedCardW may be -1 on the very first call before any state arrives;
        // fall back to a height-based estimate in that case only.
        double base = (cachedCardW > 0) ? cachedCardW
                : Math.max(effectiveH() * TILE_H_RATIO, 60.0);
        return base * (110.0 / 84.0);
    }

    // Card-width computation (call only when cache must be refreshed)

    private double computeBaseCardWidth() {
        double w = effectiveW();
        double h = effectiveH();

        // Opportunistically update cached panel heights from live measurements
        if (othersBar.getHeight() > 0) cachedOthersH = othersBar.getHeight();
        if (centralBox.getHeight() > 0) cachedCentralH = centralBox.getHeight();

        double othersH = cachedOthersH  > 0 ? cachedOthersH  : h * 0.10;
        double centralH = cachedCentralH > 0 ? cachedCentralH : h * 0.18;
        double selfH = selfPanel != null ? selfPanel.panelHeight() : 165.0;

        // Horizontal: fit n cards into the row width
        double rowW = w - 220 - 70;
        int n = Math.max(maxCardsInAnyRow(), 1);
        double wFromW = (rowW - 6.0 * (n - 1) - 40) / n;

        // Vertical: 2 rows share the available height
        double availH  = Math.max(h - OVERHEAD_H - othersH - centralH - selfH, 0);
        double wFromH  = (availH / 2.0) / CARD_ASPECT;

        return Math.max(Math.min(wFromW, wFromH), 30.0);
    }

    /** Returns the card width for the current render, using the cache. */
    private double cardWidth() {
        double base = (cachedCardW > 0) ? cachedCardW : computeBaseCardWidth();
        return base * userScale;
    }

    private double cardHeight() {
        return cardWidth() * CARD_ASPECT;
    }

    /** Total cards in the fuller row (upper = topTribe+topBuilding, lower = bottomTribe+bottomBuilding). */
    private int maxCardsInAnyRow() {
        if (lastState == null) return 8;
        int upper = lastState.getTopRowTribe().size()    + lastState.getTopRowBuilding().size();
        int lower = lastState.getBottomRowTribe().size() + lastState.getBottomRowBuilding().size();
        return Math.max(upper, lower);
    }

    /**
     * Listens to window resize and triggers a card-size recompute + re-render.
     * Debounced at 120 ms to avoid flooding during live drag-resize.
     */
    private void rescale() {
        PauseTransition debounce = new PauseTransition(Duration.millis(120));
        debounce.setOnFinished(e -> {
            if (lastState != null) {
                // Panel heights are in absolute pixels and scale with window size;
                // invalidate them so computeBaseCardWidth() uses proportional fallbacks
                // on the first pass, then re-measures correctly after layout settles.
                cachedOthersH  = -1;
                cachedCentralH = -1;
                cachedCardW    = computeBaseCardWidth();
                update(lastState);
                // Second pass: layout has now settled at the new window dimensions,
                // so we can read the real panel heights and get the correct card size.
                Platform.runLater(() -> {
                    if (lastState != null) {
                        cachedCardW = computeBaseCardWidth();
                        update(lastState);
                    }
                });
            }
        });
        stage.widthProperty().addListener((obs, old, w)  -> debounce.playFromStart());
        stage.heightProperty().addListener((obs, old, h) -> debounce.playFromStart());
    }

    /**
     *  CMD+/- to resize the gui dimensions
     */
    private void rescaleCMD(){
        overlayRoot.sceneProperty().addListener((obs, old, scene) -> {
            if (scene != null) {
                scene.setOnKeyPressed(e -> {
                    if (e.isMetaDown() || e.isControlDown() || e.isShiftDown()) {
                        if (e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.EQUALS) {
                            userScale = Math.min(userScale + 0.1, 2.0);
                            cachedCardW = -1;
                            cachedOthersH  = -1;
                            cachedCentralH = -1;
                            if (lastState != null) {
                                cachedCardW = computeBaseCardWidth();
                                update(lastState);
                                Platform.runLater(() -> {
                                    if (lastState != null) {
                                        cachedCardW = computeBaseCardWidth();
                                        update(lastState);
                                    }
                                });
                            }
                        } else if (e.getCode() == KeyCode.MINUS) {
                            userScale = Math.max(userScale - 0.1, 0.5);
                            cachedCardW = -1;
                            cachedOthersH  = -1;
                            cachedCentralH = -1;
                            if (lastState != null) {
                                cachedCardW = computeBaseCardWidth();
                                update(lastState);
                                Platform.runLater(() -> {
                                    if (lastState != null) {
                                        cachedCardW = computeBaseCardWidth();
                                        update(lastState);
                                    }
                                });
                            }
                        }
                    }
                });
            }
        });
    }

    @Override
    public void update(LocalGameState state) {
        this.lastState = state;

        int newMax = maxCardsInAnyRow();
        if (newMax != cachedMaxCards) {
            cachedMaxCards = newMax;
            cachedCardW    = computeBaseCardWidth();
        } else if (cachedCardW <= 0) {
            cachedCardW = computeBaseCardWidth();
        }

        String phase = state.getPhase();
        String me = router.playerName();
        boolean isMyTurn = me != null && me.equals(state.getCurrentPlayerName());
        Map<String, PlayerDto> playersByName = indexByName(state.getPlayers());

        updatePlayersBar(state.getPlayers(), me, state.getCurrentPlayerName());
        updateCentralBox(state, playersByName, phase, isMyTurn);
        updateRowsBox(upperRowsBox, state.getTopRowTribe(), state.getTopRowBuilding(), phase, isMyTurn);
        updateRowsBox(lowerRowsBox, state.getBottomRowTribe(), state.getBottomRowBuilding(), phase, isMyTurn);
        updateDecks(state.getCurrentEra());

        if (needsInitialMeasure) {
            needsInitialMeasure = false;
            Platform.runLater(() -> {
                cachedCardW = computeBaseCardWidth();
                if (lastState != null) update(lastState);
            });
        }
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
            node.setPickOnBounds(true);
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
                               String phase, boolean isMyTurn) {
        box.getChildren().clear();
        box.setAlignment(Pos.CENTER);

        double w = cardWidth();
        double h = cardHeight();

        box.getChildren().add(buildRow(tribeCards, phase, isMyTurn, w, h));
        box.getChildren().add(buildRow(buildingCards, phase, isMyTurn, w, h));
    }

    private HBox buildRow(List<CardDto> cards, String phase, boolean isMyTurn,
                          double cardW, double cardH) {
        boolean canDraw = ("ACTION".equals(phase) && isMyTurn)
                || ("PRE_END_OF_ROUND".equals(phase) && isMyTurn);
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER);
        for (CardDto c : cards) {
            CardView v = new CardView(c, true, cardW, cardH);
            v.setPickOnBounds(true); // full rectangular hit area, including rounded-corner transparent pixels
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

        double cw = cardWidth();
        double ch = cardHeight();

        DeckView td = new DeckView("BackEra" + eraNum + ".png", cw, ch);
        if (eraNumBuilding < 4) {
            DeckView bd = new DeckView(buildingBackForEra(eraNumBuilding), cw, ch);
            decksBox.getChildren().addAll(td, bd);
        } else {
            decksBox.getChildren().add(td);
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

}
