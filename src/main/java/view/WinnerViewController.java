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
public class WinnerViewController {

    @FXML private StackPane rootPane;
    @FXML private Label winnerNameLabel;
    @FXML private Label winnerScoreLabel;
    @FXML private VBox rankingBox;

    private SceneRouter router;

    public void bind(SceneRouter router, List<PlayerDto> players, List<String> winners) {
        this.router = router;

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
        rankLbl.setStyle("-fx-text-fill: #aab7b8; -fx-font-size: 14; -fx-font-weight: bold; -fx-min-width: 36;");

        TotemView totem = new TotemView(p.color, 22);

        Label name = new Label(p.name);
        name.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-min-width: 200;");

        Label pp = new Label(p.prestigePoints + " PP");
        pp.setStyle("-fx-text-fill: #9b59b6; -fx-font-size: 13; -fx-font-weight: bold; -fx-min-width: 80;");

        Label food = new Label(p.food + " food");
        food.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 13;");

        HBox row = new HBox(12, rankLbl, totem, name, pp, food);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #102b40; -fx-padding: 8 16 8 16; -fx-background-radius: 6;");
        return row;
    }

    public StackPane root() { return rootPane; }

    @FXML
    private void onBack() {
        router.toLobby();
    }
}
