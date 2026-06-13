package view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import shared.dto.CardDto;
import shared.dto.PlayerDto;
import shared.dto.TribeDto;
import view.widgets.FoodWidget;
import view.widgets.ImageCache;
import view.widgets.PpWidget;
import view.widgets.TotemView;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small marker around the table for each player.
 * Shows the totem, name, food/PP, and a row of chips summarising the player's tribe.
 */
public class PlayerViewController implements ViewController {

    /** Base pane of the player panel*/
    @FXML private StackPane totemSlot;
    /** Label containing the name of the player*/
    @FXML private Label nameLabel;
    /** HBox containing the food state*/
    @FXML private HBox foodBox;
    /** StackPane containing the pp state*/
    @FXML private StackPane ppBox;
    /**VBox containing the stats of cards owned by the player*/
    @FXML private VBox chipsBar;

    /** Player stat data in the form of DTO*/
    private PlayerDto player;
    /** Bool saving if it's the user player view*/
    private boolean isSelf;
    /** Bool saving if it's the player turn**/
    private boolean isCurrent;
    /** parent root*/
    private Parent root;

    /**
     * Called by the board view, it loads the player view panel.
     * @param p Player DTO
     * @param isSelf Bool saving if it's the user player view
     * @param isCurrentPlayer Bool saving if it's the player turn
     * @return the player view panel itself
     */
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

    /**
     * @return the parent root
     */
    public Parent root() { return root; }

    /**
     * @return the owning the panel player's DTO
     */
    public PlayerDto player() { return player; }

    /**
     * Called by {@code load()}, it initializes the player pane view.
     * It builds new {@link FoodWidget} and {@link PpWidget} and calls {@code buildChips()}.
     * @param p Player DTO
     * @param isSelf Bool saving if it's the user player view
     * @param isCurrent Bool saving if it's the player turn
     */
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
        foodBox.getChildren().setAll(new FoodWidget(p.food));
        ppBox.getChildren().setAll(new PpWidget(p.prestigePoints));

        buildChips(p.tribe);
    }

    /**
     * Called by {@code init()}, builds the chips showing the various cards stats and types.
     * It divides the area it owns in rows (HBox) to organize the space in a more efficient way.
     * @param tribe player's tribeDTO
     */
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

        // Row 1: HUNTER · BUILDER
        HBox row1 = new HBox(4);
        row1.setAlignment(Pos.CENTER_LEFT);
        addIconChipTo(row1, counts, "HUNTER",  "Hunter.png");
        addIconChipTo(row1, counts, "BUILDER", "Builder.png");

        // Row 2: ARTIST · INVENTOR
        HBox row2 = new HBox(4);
        row2.setAlignment(Pos.CENTER_LEFT);
        addIconChipTo(row2, counts, "ARTIST",   "Artist.png");
        addIconChipTo(row2, counts, "INVENTOR", "Inventor.png");

        // Row 3: SHAMAN (with stars)
        HBox row3 = new HBox(4);
        row3.setAlignment(Pos.CENTER_LEFT);
        if (counts.getOrDefault("SHAMAN", 0) > 0) {
            HBox chip = buildIconChip("Shaman.png", "×" + counts.get("SHAMAN"));
            if (totalStars > 0) {
                Image starImg = ImageCache.get("/images/icons/star.png");
                if (starImg != null) {
                    ImageView iv = new ImageView(starImg);
                    iv.setFitWidth(11);
                    iv.setFitHeight(11);
                    iv.setPreserveRatio(true);
                    chip.getChildren().add(iv);
                }
                chip.getChildren().add(new Label("×" + totalStars));
            }
            row3.getChildren().add(chip);
        }

        // Row 4: GATHERER · buildings
        HBox row4 = new HBox(4);
        row4.setAlignment(Pos.CENTER_LEFT);
        addIconChipTo(row4, counts, "GATHERER", "Gatherer.png");
        int buildings = tribe.buildings != null ? tribe.buildings.size() : 0;
        if (buildings > 0) {
            row4.getChildren().add(buildTextChip("⌂ ×" + buildings));
        }

        if (!row1.getChildren().isEmpty()) chipsBar.getChildren().add(row1);
        if (!row2.getChildren().isEmpty()) chipsBar.getChildren().add(row2);
        if (!row3.getChildren().isEmpty()) chipsBar.getChildren().add(row3);
        if (!row4.getChildren().isEmpty()) chipsBar.getChildren().add(row4);
    }

    /**
     * Add iconChip to the specified row.
     * @param row row in which to insert the chip
     * @param counts number of cards
     * @param type type of the card
     * @param iconFile the icon image.
     */
    private void addIconChipTo(HBox row, Map<String, Integer> counts, String type, String iconFile) {
        int n = counts.getOrDefault(type, 0);
        if (n == 0) return;
        row.getChildren().add(buildIconChip(iconFile, "×" + n));
    }

    /**
     * Called by {@code init}, builds the different icon chips in a standard way to make them similar.
     * If it does not find the icon uses text instead.
     * @param iconFile the icon of the type of the card
     * @param text the name of the card
     * @return HBox that is the chip
     */
    private HBox buildIconChip(String iconFile, String text) {
        HBox box = new HBox(4);
        box.getStyleClass().add("mesos-card-chip");
        box.setAlignment(Pos.CENTER);

        Image img = ImageCache.get("/images/icons/" + iconFile);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(14);
            iv.setFitHeight(14);
            iv.setPreserveRatio(true);
            box.getChildren().add(iv);
        }
        box.getChildren().add(new Label(text));
        return box;
    }

    /** called by {@code buildIconCHip()} if the icon image is not found. Put text in the chip instead */
    private HBox buildTextChip(String text) {
        HBox box = new HBox(new Label(text));
        box.getStyleClass().add("mesos-card-chip");
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /**Helper method to count the shaman stars*/
    private static int countStars(String details) {
        int count = 0;
        for (int i = 0; i < details.length(); i++) {
            if (details.charAt(i) == '★') count++;
        }
        return count;
    }
}
