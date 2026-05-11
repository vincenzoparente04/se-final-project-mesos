package view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import shared.dto.PlayerDto;
import view.widgets.TotemView;

import java.io.IOException;

/**
 * The little marker shown around the table for each player.
 * For self ({@code isSelf=true}) the click handler is suppressed and
 * the caller is expected to embed the marker plus the tribe under it.
 */
public class PlayerViewController {

    @FXML private StackPane totemSlot;
    @FXML private Label nameLabel;
    @FXML private Label foodLabel;
    @FXML private Label ppLabel;

    private PlayerDto player;
    private boolean isSelf;
    private boolean isCurrent;
    private Parent root;

    public static PlayerViewController load(PlayerDto p, boolean isSelf, boolean isCurrentPlayer) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    PlayerViewController.class.getResource("/org/example/mesos/player-view.fxml"));
            Parent r = loader.load();
            PlayerViewController ctrl = loader.getController();
            ctrl.root = r;
            ctrl.init(p, isSelf, isCurrentPlayer);
            return ctrl;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load player-view.fxml", e);
        }
    }

    public Parent root() { return root; }
    public PlayerDto player() { return player; }

    private void init(PlayerDto p, boolean isSelf, boolean isCurrent) {
        this.player = p;
        this.isSelf = isSelf;
        this.isCurrent = isCurrent;

        totemSlot.getChildren().setAll(new TotemView(p.color, 40));
        nameLabel.setText(p.name + (isSelf ? "  (you)" : ""));
        foodLabel.setText("Food: " + p.food);
        ppLabel.setText("PP: " + p.prestigePoints);

        if (isCurrent) {
            nameLabel.setStyle(nameLabel.getStyle() + " -fx-text-fill: #f1c40f;");
        }
    }
}
