package view.widgets;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import shared.dto.OfferTileDto;
import shared.dto.PlayerDto;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;

/**
 * One offer-track tile with the (optional) occupant's totem overlaid.
 * Clickable when {@code clickable} is true; click emits the tile's letter.
 */
public class OfferTileView extends StackPane {

    public static final double TILE_WIDTH = 110;
    public static final double TILE_HEIGHT = 110;

    public OfferTileView(OfferTileDto tile,
                         Map<String, PlayerDto> playersByName,
                         boolean clickable,
                         Consumer<Character> onClick) {
        String path = "/images/TileCards/OfferTrail_" + tile.letter + "_FRONT.png";
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream != null) {
            ImageView iv = new ImageView(new Image(stream));
            iv.setFitWidth(TILE_WIDTH);
            iv.setFitHeight(TILE_HEIGHT);
            iv.setPreserveRatio(true);
            getChildren().add(iv);
        } else {
            Rectangle r = new Rectangle(TILE_WIDTH, TILE_HEIGHT, Color.BEIGE);
            r.setStroke(Color.DARKGOLDENROD);
            getChildren().add(r);
        }

        if (tile.occupantName != null) {
            PlayerDto p = playersByName.get(tile.occupantName);
            String color = p != null ? p.color : "WHITE";
            TotemView totem = new TotemView(color, 40);
            getChildren().add(totem);
        }

        setMinSize(TILE_WIDTH, TILE_HEIGHT);
        setPrefSize(TILE_WIDTH, TILE_HEIGHT);

        if (clickable && tile.occupantName == null) {
            setStyle("-fx-cursor: hand;");
            setOnMouseClicked(e -> onClick.accept(tile.letter));
        }
    }
}
