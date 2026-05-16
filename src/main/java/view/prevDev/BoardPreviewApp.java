package view.prevDev;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import network.client.LocalGameState;
import network.client.VirtualServer;
import shared.dto.*;
import view.SceneController;
import view.SceneRouter;

import java.io.IOException;
import java.util.List;

/**
 * Standalone launcher that boots directly into the board view with fake data.
 * Run this class instead of ClientMain to iterate on board UI without a server.
 */
public class BoardPreviewApp extends Application {

    private static final String ME = "Preview";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        LocalGameState state = new LocalGameState();
        state.update(buildFakeState());

        VirtualServer noop = new NoopVirtualServer();

        SceneRouter router = new SceneRouter(stage, state, null) {
            @Override public String playerName()        { return ME; }
            @Override public VirtualServer getVirtualServer() { return noop; }
            @Override public void toLobby()             {}
            @Override public void toWinner(List<PlayerDto> p, List<String> w) {}
            @Override public void toSplash()            {}
            @Override public void toNick()              {}
            @Override public void toNetworkSetup()      {}
        };

        loadBoard(router, stage);
    }

    // ── Board bootstrap ────────────────────────────────────────────────────

    private static void loadBoard(SceneRouter router, Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    BoardPreviewApp.class.getResource("/org/example/mesos/board/board-view.fxml"));
            loader.load();
            SceneController ctrl = loader.getController();

            StackPane root = ctrl.root();
            ctrl.bind(router);

            Scene scene = new Scene(root, 1280, 800);
            String css = BoardPreviewApp.class
                    .getResource("/styles/mesos.css").toExternalForm();
            scene.getStylesheets().add(css);

            stage.setTitle("Board Preview");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load board FXML", e);
        }
    }

    // ── Fake game state ────────────────────────────────────────────────────

    private static GameStateDto buildFakeState() {
        List<PlayerDto> players = List.of(
                new PlayerDto(ME,      12, 8,  "BLUE",   "A", tribe(3, 1)),
                new PlayerDto("Alice", 7,  14, "RED",    "B", tribe(2, 2)),
                new PlayerDto("Bob",   5,  6,  "GREEN",  "C", tribe(1, 0))
        );

        List<OfferTileDto> offerTiles = List.of(
                new OfferTileDto('A', "DRAW_CARDS", null,    2, 2, 0, 0),
                new OfferTileDto('B', "DRAW_CARDS", "Alice", 2, 1, 1, 0),
                new OfferTileDto('C', "TAKE_FOOD",  null,    null, null, null, null),
                new OfferTileDto('D', "DRAW_CARDS", "Bob",   1, 2, 0, 1),
                new OfferTileDto('E', "TAKE_FOOD",  null,    null, null, null, null)
        );

        List<TurnOrderSlotDto> turnOrder = List.of(
                new TurnOrderSlotDto(0, ME),
                new TurnOrderSlotDto(1, "Alice"),
                new TurnOrderSlotDto(2, "Bob")
        );

        List<CardDto> topRowTribe       = cards("CHARACTER", "ERA_I", 31, 32, 33, 34); //20, 21, 22, 23
        List<CardDto> bottomRowTribe    = cards("CHARACTER", "ERA_I", 35, 36, 37, 38); //24, 25, 26, 27
        List<CardDto> topRowBuilding    = cards("BUILDING",  "ERA_I", 21, 20 );
        List<CardDto> bottomRowBuilding = cards("BUILDING",  "ERA_I", 21);

        return new GameStateDto(
                "ACTION",
                ME,
                2,
                "ERA_I",
                players,
                offerTiles,
                turnOrder,
                topRowTribe,
                bottomRowTribe,
                topRowBuilding,
                bottomRowBuilding,
                null
        );
    }

    private static TribeDto tribe(int charCount, int buildingCount) {
        List<CardDto> chars     = cards("CHARACTER", "ERA_I", intRange(110, charCount));
        List<CardDto> buildings = cards("BUILDING",  "ERA_I", intRange(100, buildingCount));
        return new TribeDto(chars, buildings);
    }

    private static List<CardDto> cards(String type, String era, int... ids) {
        List<CardDto> list = new java.util.ArrayList<>();
        for (int id : ids) list.add(card(id, type, era));
        return list;
    }

    private static int[] intRange(int start, int count) {
        int[] arr = new int[count];
        for (int i = 0; i < count; i++) arr[i] = start + i;
        return arr;
    }

    private static CardDto card(int id, String type, String era) {
        int imageId = 20 + (id % 20);
        return new CardDto(
                id, type, era,
                type.equals("BUILDING") ? 3 : 0,
                type.equals("BUILDING") ? 2 : 0,
                null,
                "FrontCard" + imageId + ".png",
                "BackEra1.png"
        );
    }
}
