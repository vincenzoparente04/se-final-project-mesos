package view;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import shared.dto.PlayerDto;
import view.widgets.TotemView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * End-of-game screen built from the last {@link PlayerDto} list received from the server,
 * with the winner names taken from the {@code GameStateDto.winners} list to handle ties.
 */
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

    public void showWinners(List<PlayerDto> players, List<String> winners) {
        List<PlayerDto> ordered = new ArrayList<>(players);
        ordered.sort(Comparator.comparingInt((PlayerDto p) -> p.prestigePoints).reversed());

        if (winners != null && !winners.isEmpty()) {
            winnerNameLabel.setText(String.join("  •  ", winners));
            PlayerDto top = ordered.isEmpty() ? null : ordered.get(0);
            winnerScoreLabel.setText(top != null
                    ? "with " + top.prestigePoints + " PP"
                    : "");
        } else if (!ordered.isEmpty()) {
            winnerNameLabel.setText(ordered.get(0).name);
            winnerScoreLabel.setText("with " + ordered.get(0).prestigePoints + " PP");
        }

        rankingBox.getChildren().clear();
        int rank = 1;
        for (PlayerDto p : ordered) {
            rankingBox.getChildren().add(buildRow(rank++, p));
        }
    }

    private HBox buildRow(int rank, PlayerDto p) {
        Label rankLbl = new Label("#" + rank);
        rankLbl.getStyleClass().add("mesos-label-bold");
        rankLbl.setMinWidth(40);

        TotemView totem = new TotemView(p.color, 24);

        Label name = new Label(p.name);
        name.getStyleClass().add("mesos-label-bold");
        name.setMinWidth(200);

        Label pp = new Label(p.prestigePoints + " PP");
        pp.getStyleClass().add("mesos-label-glow");
        pp.setMinWidth(80);

        Label food = new Label(p.food + " food");
        food.getStyleClass().add("mesos-label");

        HBox row = new HBox(12, rankLbl, totem, name, pp, food);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("mesos-tribe-group");
        return row;
    }

    public StackPane root() { return rootPane; }

    @FXML
    private void onBack() {
        router.toLobby();
    }
}
