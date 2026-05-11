package view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import shared.dto.CardDto;
import shared.dto.PlayerDto;
import shared.dto.TribeDto;
import view.widgets.TotemView;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small marker around the table for each player.
 * Shows the totem, name, food/PP, and a row of chips summarising the player's tribe
 * (e.g. "H×2", "S 4★", "Bldg ×1") so opponents are readable at a glance.
 */
public class PlayerViewController {

    @FXML private StackPane totemSlot;
    @FXML private Label nameLabel;
    @FXML private Label foodLabel;
    @FXML private Label ppLabel;
    @FXML private FlowPane chipsBar;

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

        totemSlot.getChildren().setAll(new TotemView(p.color, 42));
        nameLabel.setText(p.name + (isSelf ? "  (you)" : ""));
        if (isCurrent) {
            nameLabel.getStyleClass().add("mesos-player-name-current");
            root.getStyleClass().add("mesos-player-marker-current");
        }
        foodLabel.setText("food " + p.food);
        ppLabel.setText("PP " + p.prestigePoints);

        buildChips(p.tribe);
    }

    private void buildChips(TribeDto tribe) {
        chipsBar.getChildren().clear();
        if (tribe == null) return;

        Map<String, Integer> counts = new LinkedHashMap<>();
        int totalStars = 0;
        if (tribe.characterCards != null) {
            for (CardDto c : tribe.characterCards) {
                counts.merge(c.type, 1, Integer::sum);
                if ("SHAMAN".equals(c.type) && c.details != null) {
                    totalStars += countStars(c.details);
                }
            }
        }

        addChip(counts, "HUNTER",   "H");
        addChip(counts, "BUILDER",  "B");
        if (counts.getOrDefault("SHAMAN", 0) > 0) {
            chipsBar.getChildren().add(buildChip("S " + counts.get("SHAMAN")
                    + (totalStars > 0 ? "  " + totalStars + "★" : "")));
        }
        addChip(counts, "ARTIST",   "A");
        addChip(counts, "INVENTOR", "I");
        addChip(counts, "GATHERER", "G");

        int buildings = tribe.buildings != null ? tribe.buildings.size() : 0;
        if (buildings > 0) {
            chipsBar.getChildren().add(buildChip("Bldg ×" + buildings));
        }
    }

    private void addChip(Map<String, Integer> counts, String type, String shortLabel) {
        int n = counts.getOrDefault(type, 0);
        if (n == 0) return;
        chipsBar.getChildren().add(buildChip(shortLabel + "×" + n));
    }

    private HBox buildChip(String text) {
        Label l = new Label(text);
        HBox box = new HBox(l);
        box.getStyleClass().add("mesos-card-chip");
        return box;
    }

    private static int countStars(String details) {
        int count = 0;
        for (int i = 0; i < details.length(); i++) {
            if (details.charAt(i) == '★') count++;
        }
        return count;
    }
}
