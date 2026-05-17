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

    //TILE_WIDTH???
    public static final double TILE_HEIGHT = 110;
    public static final double TILE_WIDTH  = TILE_HEIGHT * (300.0 / 463.0);

    public TurnOrderTileView(List<TurnOrderSlotDto> slots,
                             Map<String, PlayerDto> playersByName) {
        this(slots, playersByName, TILE_HEIGHT);
    }

    public TurnOrderTileView(List<TurnOrderSlotDto> slots,
                             Map<String, PlayerDto> playersByName,
                             double tileHeight) {
        double tileWidth = tileHeight * (300.0 / 463.0);
        int playerCount = slots.size();
        String path = "/images/TileCards/Order_" + playerCount + "_Players_FRONT.png";
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream != null) {
            ImageView iv = new ImageView(new Image(stream));
            iv.setFitHeight(tileHeight);
            iv.setPreserveRatio(true);
            getChildren().add(iv);
        } else {
            Rectangle r = new Rectangle(tileWidth, tileHeight, Color.web("#fdebd0"));
            r.setStroke(Color.web("#a04000"));
            getChildren().add(r);
        }

        /*TODO: REMOVE DEAD CODE
        VBox totems = new VBox(-10); // Negative spacing pushes them slightly together if needed, or adjust to match circles
        totems.setAlignment(Pos.TOP_CENTER);
        // Add padding to push the VBox down exactly to where the first circle starts on the image.
        // We will adjust the top padding (e.g. 15% of the total height)
        totems.setPadding(new javafx.geometry.Insets(TILE_HEIGHT * 0.15, 0, 0, 0)); 
        
        for (TurnOrderSlotDto slot : slots) {
            StackPane cell = new StackPane();
            // Match the height to the distance between circles on the card image
            cell.setPrefSize(TILE_WIDTH, TILE_HEIGHT / slots.size() * 0.7); 
            if (slot.occupantName != null) {
                PlayerDto p = playersByName.get(slot.occupantName);
                String color = p != null ? p.color : "WHITE";
                cell.getChildren().add(new TotemView(color, 20)); // Size the totem to fit the circle
            }
            totems.getChildren().add(cell);
        }
        getChildren().add(totems);

         */

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

        getChildren().add(totemLayer);

        setMinSize(tileWidth, tileHeight);
        setPrefSize(tileWidth, tileHeight);
    }
}
