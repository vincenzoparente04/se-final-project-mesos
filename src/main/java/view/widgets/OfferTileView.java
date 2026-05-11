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

    public static final double TILE_HEIGHT = 110;

    public OfferTileView(OfferTileDto tile,
                         Map<String, PlayerDto> playersByName,
                         boolean clickable,
                         Consumer<Character> onClick) {
        String path = "/images/TileCards/OfferTrail_" + tile.letter + "_FRONT.png";
        InputStream stream = getClass().getResourceAsStream(path);
        
        double actualWidthStr = 300;
        double actualHeightStr = 485;
        
        if (stream != null) {
            Image img = new Image(stream);
            actualWidthStr = img.getWidth();
            actualHeightStr = img.getHeight();
            
            ImageView iv = new ImageView(img);
            iv.setFitHeight(TILE_HEIGHT);
            iv.setPreserveRatio(true);
            getChildren().add(iv);
        } else {
            Rectangle r = new Rectangle(68, TILE_HEIGHT, Color.BEIGE);
            r.setStroke(Color.DARKGOLDENROD);
            getChildren().add(r);
        }

        double calculatedWidth = TILE_HEIGHT * (actualWidthStr / actualHeightStr);

        if (tile.occupantName != null) {
            PlayerDto p = playersByName.get(tile.occupantName);
            String color = p != null ? p.color : "WHITE";
            TotemView totem = new TotemView(color, 40);
            
            // Adjust the margin calculation based on this specific image's aspect ratio
            double offsetProportion = 150.0 / actualHeightStr; 
            javafx.geometry.Insets offset = new javafx.geometry.Insets(TILE_HEIGHT * offsetProportion - 40.0, 0, 0, 0);
            StackPane.setAlignment(totem, javafx.geometry.Pos.TOP_CENTER);
            StackPane.setMargin(totem, offset);

            getChildren().add(totem);
        }

        setMinSize(calculatedWidth, TILE_HEIGHT);
        setPrefSize(calculatedWidth, TILE_HEIGHT);

        if (clickable && tile.occupantName == null) {
            setStyle("-fx-cursor: hand;");
            setOnMouseClicked(e -> onClick.accept(tile.letter));
        }
    }
}
