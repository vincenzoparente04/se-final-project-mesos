package view.widgets;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
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

    public static final double TILE_WIDTH = 320;
    public static final double TILE_HEIGHT = 110;

    public TurnOrderTileView(List<TurnOrderSlotDto> slots,
                             Map<String, PlayerDto> playersByName) {
        int playerCount = slots.size();
        String path = "/images/TileCards/Order_" + playerCount + "_Players_FRONT.png";
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream != null) {
            ImageView iv = new ImageView(new Image(stream));
            iv.setFitWidth(TILE_WIDTH);
            iv.setFitHeight(TILE_HEIGHT);
            iv.setPreserveRatio(true);
            getChildren().add(iv);
        } else {
            Rectangle r = new Rectangle(TILE_WIDTH, TILE_HEIGHT, Color.web("#fdebd0"));
            r.setStroke(Color.web("#a04000"));
            getChildren().add(r);
        }

        HBox totems = new HBox(8);
        totems.setAlignment(Pos.CENTER);
        for (TurnOrderSlotDto slot : slots) {
            StackPane cell = new StackPane();
            cell.setPrefSize(50, 60);
            if (slot.occupantName != null) {
                PlayerDto p = playersByName.get(slot.occupantName);
                String color = p != null ? p.color : "WHITE";
                cell.getChildren().add(new TotemView(color, 40));
            }
            totems.getChildren().add(cell);
        }
        getChildren().add(totems);

        setMinSize(TILE_WIDTH, TILE_HEIGHT);
        setPrefSize(TILE_WIDTH, TILE_HEIGHT);
    }
}
