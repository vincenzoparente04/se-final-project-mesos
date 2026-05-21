package view;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import shared.dto.PlayerDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.PlayerScoringDeltaDto;
import view.widgets.TotemView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WinnerViewController implements SceneController {

    @FXML private StackPane rootPane;
    @FXML private Label winnerNameLabel;
    @FXML private Label winnerScoreLabel;
    @FXML private VBox rankingBox;

    private SceneRouter router;

    @Override
    public void bind(SceneRouter router) {
        this.router = router;
    }

    // Called without scoring (fallback)

    @Override
    public void showWinners(List<PlayerDto> players, List<String> winners) {
        showWinners(players, winners, null);
    }

    //Main entry point

    @Override
    public void showWinners(List<PlayerDto> players, List<String> winners, EndGameScoringDto scoring) {
        List<PlayerDto> ordered = new ArrayList<>(players);
        ordered.sort(Comparator.comparingInt((PlayerDto p) -> p.prestigePoints).reversed());

        if (winners != null && !winners.isEmpty()) {
            winnerNameLabel.setText(String.join("  •  ", winners));
            PlayerDto top = ordered.isEmpty() ? null : ordered.get(0);
            winnerScoreLabel.setText(top != null ? top.prestigePoints + " PP" : "");
        } else if (!ordered.isEmpty()) {
            winnerNameLabel.setText(ordered.get(0).name);
            winnerScoreLabel.setText(ordered.get(0).prestigePoints + " PP");
        }

        rankingBox.getChildren().clear();

        Map<String, PlayerScoringDeltaDto> scoringByName = scoring != null
                ? scoring.deltas.stream().collect(Collectors.toMap(d -> d.playerName, d -> d))
                : Map.of();

        if (!scoringByName.isEmpty()) {
            rankingBox.getChildren().add(buildTableHeader());
        }

        int rank = 1;
        for (PlayerDto p : ordered) {
            PlayerScoringDeltaDto delta = scoringByName.get(p.name);
            rankingBox.getChildren().add(buildRow(rank++, p, delta));
        }
    }

    // Table header

    private HBox buildTableHeader() {
        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("mesos-winner-header");

        header.getChildren().addAll(
                headerCell("#",    32),
                headerCell("",     30),   // totem placeholder
                headerCell("Player", 160),
                headerCell("Builders",  70),
                headerCell("Artist",  70),
                headerCell("Inventor",  70),
                headerCell("building PP",   70),
                headerCell("building special PP",   70),
                headerCell("Total",   80)
        );
        return header;
    }

    private Label headerCell(String text, double w) {
        Label l = new Label(text);
        l.getStyleClass().add("mesos-winner-header-cell");
        l.setMinWidth(w);
        l.setPrefWidth(w);
        return l;
    }

    // Player row

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

    private Label cell(String text, double w) {
        Label l = new Label(text);
        l.setMinWidth(w);
        l.setPrefWidth(w);
        return l;
    }

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

    private Label totalCell(int pp) {
        Label l = new Label(pp + " PP");
        l.setMinWidth(80);
        l.setPrefWidth(80);
        l.getStyleClass().add("mesos-label-glow");
        return l;
    }

    public StackPane root() { return rootPane; }

    @FXML
    private void onBack() {
        if (router.getVirtualServer() != null) {
            router.getVirtualServer().sendLeaveCommand();
        }
        router.toLobby();
    }
}
