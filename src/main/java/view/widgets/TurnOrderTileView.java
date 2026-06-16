package view.widgets;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import shared.dto.PlayerDto;
import shared.dto.TurnOrderSlotDto;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * The TurnOrderTile of the board, showing the n-player tile background
 * and any totems currently sitting on its slots.
 * The tile image is read as {@code Order_<n>_Players_FRONT.png}.
 */
public class TurnOrderTileView extends StackPane {

    /**Height of the turn order tile, for proportion*/
    public static final double TILE_HEIGHT = 110;

    /**
     * Main method of this class renderds and displays the single turn order tile image view
     * @param slots List of DTOs of the slots
     * @param playersByName Map of players and their corresponding DTOs.
     * @param tileHeight the height of the tile to build
     */
    public TurnOrderTileView(List<TurnOrderSlotDto> slots,
                             Map<String, PlayerDto> playersByName,
                             double tileHeight) {
        double tileWidth = tileHeight * (300.0 / 463.0);
        int playerCount = slots.size();
        String path = "/images/TileCards/Order_" + playerCount + "_Players_FRONT.png";
        Image img = ImageCache.get(path);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitHeight(tileHeight);
            iv.setPreserveRatio(true);
            getChildren().add(iv);
        } else {
            Rectangle r = new Rectangle(tileWidth, tileHeight, Color.web("#fdebd0"));
            r.setStroke(Color.web("#a04000"));
            getChildren().add(r);
        }

        Pane totemLayer = new Pane();
        totemLayer.setPrefSize(tileWidth, tileHeight);

        int numPlayers = slots.size();
        double[] yOffsets = switch (numPlayers) {
            case 2 -> new double[]{145.0/462.0, 222.0/462.0};
            case 3 -> new double[]{122.0/462.0, 202.0/462.0, 282.0/462.0};
            case 4 -> new double[]{104.0/462.0, 184.0/462.0, 262.0/462.0, 341.0/462.0};
            case 5 -> new double[]{74.0/462.0, 151.0/462.0, 231.0/462.0, 312.0/462.0, 390.0/462.0};
            default -> new double[numPlayers];
        };

        double scale = tileHeight / TILE_HEIGHT;
        double boxHeight = tileHeight * 0.15;
        double totemSize = 38.0 * scale;

        for (int i = 0; i < slots.size(); i++) {
            TurnOrderSlotDto slot = slots.get(i);
            if (slot.occupantName == null) continue;

            PlayerDto p = playersByName.get(slot.occupantName);
            String color = p != null ? p.color : "WHITE";
            TotemView totem = new TotemView(color, (int) Math.round(totemSize));

            StackPane cell = new StackPane(totem);
            cell.setPrefHeight(boxHeight);
            cell.setPrefWidth(tileWidth);
            cell.setMinWidth(tileWidth);
            cell.setMaxWidth(tileWidth);
            cell.setLayoutX(0);
            cell.setAlignment(Pos.BOTTOM_CENTER);
            cell.setLayoutY(tileHeight * yOffsets[i] - totemSize);

            totemLayer.getChildren().add(cell);
        }

        totemLayer.setMouseTransparent(true);
        getChildren().add(totemLayer);

        setMinSize(tileWidth, tileHeight);
        setPrefSize(tileWidth, tileHeight);
        setMaxSize(tileWidth, tileHeight);
        setPickOnBounds(true);
    }
}
