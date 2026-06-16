package view;

import database.ScoreRecord;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import shared.dto.PlayerDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.PlayerScoringDeltaDto;
import view.widgets.LeaderboardOverlay;
import view.widgets.TotemView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Last screen of the GUI. Showed after the normal end or early end of the game. Shows the winner(s) and the ranking of all players with their scoring breakdown.
 * If the leaderboard DB is available, it also shows a button to open the leaderboard overlay.
 * It implements the {@link SceneController} interface to be able to be called by the {@link SceneRouter} when the game ends.
 */
public class WinnerViewController implements SceneController {

    /** The root pane of the winner view screen layout. */
    @FXML private StackPane rootPane;
    /** Label that holds the name of the winner */
    @FXML private Label winnerNameLabel;
    /* Label that holds the score of the winner */
    @FXML private Label winnerScoreLabel;
    /** VBox of the other players rankings */
    @FXML private VBox rankingBox;
    /** Button to open the leaderboard DB*/
    @FXML private Button leaderboardBtn;
    /** The router used to navigate between scenes. */
    private SceneRouter router;

    /**List to store leaderboard data*/
    private List<ScoreRecord> leaderboardData;

    private int leaderboardRank;
    private int leaderboardPoints;

    /**
     * Binds the router to this controller when this controller is created to make it possible to reference it.
     * @param router the SceneRouter
     */
    @Override
    public void bind(SceneRouter router) {
        this.router = router;
    }

    // Called without scoring (fallback)

    /**
     * Called when the game ends early. There is no EndGameScoringDTO
     * @param players the list of players in the game
     * @param winners the list of winner usernames
     */
    @Override
    public void showWinners(List<PlayerDto> players, List<String> winners) {
        showWinners(players, winners, null);
    }

    //Main entry point

    /**
     *
     * @param players the list of players in the game
     * @param winners the list of winner usernames
     * @param scoring the detailed scoring breakdown for all players
     */
    @Override
    public void showWinners(List<PlayerDto> players, List<String> winners, EndGameScoringDto scoring) {
        
        List<PlayerDto> actualPlayers = players != null ? players : router.localState().getPlayers();
        
        if (scoring != null) {
            List<PlayerScoringDeltaDto> ordered = new ArrayList<>(scoring.deltas);
            ordered.sort(Comparator.comparingInt((PlayerScoringDeltaDto p) -> p.prestigeAfter).reversed());
            
            int topScore = ordered.isEmpty() ? 0 : ordered.get(0).prestigeAfter;
            List<String> calculatedWinners = new ArrayList<>();
            if (!ordered.isEmpty()) {
                for (PlayerScoringDeltaDto p : ordered) {
                    if (p.prestigeAfter == topScore) {
                        calculatedWinners.add(p.playerName);
                    }
                }
            }

            if (!calculatedWinners.isEmpty()) {
                winnerNameLabel.setText(String.join("  •  ", calculatedWinners));
                winnerScoreLabel.setText(ordered.get(0).prestigeAfter + " PP");
            }

            rankingBox.getChildren().clear();

            Map<String, PlayerScoringDeltaDto> scoringByName = scoring.deltas.stream()
                    .collect(Collectors.toMap(d -> d.playerName, d -> d));

            if (!scoringByName.isEmpty()) {
                rankingBox.getChildren().add(buildTableHeader());
            }

            Map<String, PlayerDto> playerInfos = actualPlayers.stream()
                    .collect(Collectors.toMap(p -> p.name, p -> p));

            int rank = 1;
            for (PlayerScoringDeltaDto d : ordered) {
                PlayerDto p = playerInfos.get(d.playerName);
                if (p != null) {
                    rankingBox.getChildren().add(buildRow(rank++, p, d));
                }
            }
        } else {
            if (winners != null && !winners.isEmpty()) {
                winnerNameLabel.setText(String.join("  •  ", winners));
                PlayerDto top = actualPlayers.stream()
                        .filter(p -> p.name.equals(winners.get(0)))
                        .findFirst().orElse(null);
                winnerScoreLabel.setText(top != null ? top.prestigePoints + " PP" : "");
            }
            rankingBox.getChildren().clear();
            
            List<PlayerDto> orderedPlayers = new ArrayList<>(actualPlayers);
            orderedPlayers.sort(Comparator.comparingInt((PlayerDto p) -> p.prestigePoints).reversed());

            int rank = 1;
            for (PlayerDto p : orderedPlayers) {
                rankingBox.getChildren().add(buildRow(rank++, p, null));
            }
        }
    }

    // Table header---------------------------------

    /**
     * Called by {@code ShowWinners()} in order to build the header of the table of endgame points
     * @return HBox of the header of the table
     */
    private HBox buildTableHeader() {
        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("mesos-winner-header");

        header.getChildren().addAll(
                headerCell("#",32),
                headerCell("",30),   // totem placeholder
                headerCell("Player", 160),
                headerCell("Builders",70),
                headerCell("Artist", 70),
                headerCell("Inventor", 70),
                headerCell("building PP", 70),
                headerCell("building special PP",70),
                headerCell("Total", 80)
        );
        return header;
    }

    /**
     * Called by {@code buildTableHeader()} in order to build a single header cell of the table of endgame points
     * @param text the text to display in the header cell
     * @param w dimension of the cell
     * @return label header Cell
     */
    private Label headerCell(String text, double w) {
        Label l = new Label(text);
        l.getStyleClass().add("mesos-winner-header-cell");
        l.setMinWidth(w);
        l.setPrefWidth(w);
        return l;
    }

    // Player row----------------------------------------


    /**
     * Called by {@code showWinners()}, it builds the row of the player in the table of endgame points
     * @param rank position of the player in the ranking
     * @param p DTO of the player
     * @param delta scoring delta of the player
     * @return HBox containing the row
     */
    private HBox buildRow(int rank, PlayerDto p, PlayerScoringDeltaDto delta) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("mesos-winner-row");
        if (rank == 1) row.getStyleClass().add("mesos-winner-row-first");

        Label rankLbl = cell("#" + rank, 32);
        rankLbl.getStyleClass().add("mesos-winner-rank");

        HBox totemBox = new HBox(new TotemView(p.color, 20));
        totemBox.setMinWidth(30);
        totemBox.setPrefWidth(30);
        totemBox.setAlignment(Pos.CENTER);

        Label name = cell(p.name, 160);
        name.getStyleClass().add("mesos-label-bold");

        row.getChildren().addAll(rankLbl, totemBox, name);

        if (delta != null) {
            row.getChildren().addAll(
                    scoringCell(delta.buildersPoints),
                    scoringCell(delta.artistsPoints),
                    scoringCell(delta.inventorsPoints),
                    scoringCell(delta.buildingPrintedPoints),
                    scoringCell(delta.endGameBuildingEffectsPoints),
                    totalCell(delta.prestigeAfter)
            );
        } else {
            Label pp = cell(p.prestigePoints + " PP", 80);
            pp.getStyleClass().add("mesos-label-glow");
            row.getChildren().add(pp);
        }

        return row;
    }

    /**
     * Called by {@code buildRow()} in order to build the rank cell, the player name cell and the total points cell of the table of endgame points
     * @param text What is written in the cell
     * @param w dimension of the cell
     * @return a label that is the cell
     */
    private Label cell(String text, double w) {
        Label l = new Label(text);
        l.setMinWidth(w);
        l.setPrefWidth(w);
        return l;
    }

    /**
     * Called by {@code buildRow()} in order to build the cells of the scoring breakdown of the table of endgame points
     * @param value points of the single cell
     * @return a label containing the points that is the cell
     */
    private Label scoringCell(int value) {
        Label l = new Label(value > 0 ? "+" + value : (value == 0 ? "—" : String.valueOf(value)));
        l.setMinWidth(70);
        l.setPrefWidth(70);
        l.setAlignment(Pos.CENTER);
        l.getStyleClass().add(value > 0 ? "mesos-winner-delta-pos"
                            : value < 0 ? "mesos-winner-delta-neg"
                            : "mesos-winner-delta-zero");
        return l;
    }

    /**
     * Called by {@code buildRow()} in order to build the total points cell of the table of endgame points,
     * it has a different style from the other cells of the scoring breakdown
     * @param pp total pp of the player
     * @return a label that is the total scoring point cell
     */
    private Label totalCell(int pp) {
        Label l = new Label(pp + " PP");
        l.setMinWidth(80);
        l.setPrefWidth(80);
        l.getStyleClass().add("mesos-label-glow");
        return l;
    }

    /** Called by SceneRouter when DB leaderboard data arrives (may be before or after showWinners). */
    public void applyLeaderboard(List<ScoreRecord> data, int rank, int points) {
        leaderboardData = data;
        leaderboardRank = rank;
        leaderboardPoints = points;
        if (leaderboardBtn != null) leaderboardBtn.setDisable(false);
    }

    /**
     * called by the {@link SceneRouter} in the loadFXML method.
     * @return the root StackPane.
     */
    public StackPane root() { return rootPane; }

    /**
     * Called when the 'show leaderboard' button is clicked.
     * Shows the leaderboard overlay with the data from the DB.
     */
    @FXML
    private void onLeaderboard() {
        if (leaderboardData == null || leaderboardData.isEmpty()) return;
        String myName = router.playerName();
        LeaderboardOverlay.show(rootPane, leaderboardData, leaderboardRank, leaderboardPoints, myName);
    }

    /**
     * Called when the 'back' button is clicked, it goes back to the lobby scene, and it notifies the server of doing so.
     */
    @FXML
    private void onBack() {
        if (router.getVirtualServer() != null) {
            router.getVirtualServer().sendLeaveCommand();
        }
        router.toLobby();
    }
}
