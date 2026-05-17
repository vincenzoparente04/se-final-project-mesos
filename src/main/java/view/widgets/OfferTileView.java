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

    //TODO: Remove dead code
    /*
    public OfferTileView(OfferTileDto tile,
                         Map<String, PlayerDto> playersByName,
                         boolean clickable,
                         Consumer<Character> onClick) {
        this(tile, playersByName, clickable, onClick, TILE_HEIGHT);
    }*/

    public OfferTileView(OfferTileDto tile,
                         Map<String, PlayerDto> playersByName,
                         boolean clickable,
                         Consumer<Character> onClick,
                         double tileHeight) {
        String path = "/images/TileCards/OfferTrail_" + tile.letter + "_FRONT.png";
        InputStream stream = getClass().getResourceAsStream(path);

        double actualW = 300, actualH = 485;

        if (stream != null) {
            Image img = new Image(stream);
            actualW = img.getWidth();
            actualH = img.getHeight();

            ImageView iv = new ImageView(img);
            iv.setFitHeight(tileHeight);
            iv.setPreserveRatio(true);
            getChildren().add(iv);
        } else {
            double fallbackW = tileHeight * (300.0 / 485.0);
            Rectangle r = new Rectangle(fallbackW, tileHeight, Color.BEIGE);
            r.setStroke(Color.DARKGOLDENROD);
            getChildren().add(r);
        }

        double calculatedWidth = tileHeight * (actualW / actualH);

        if (tile.occupantName != null) {
            PlayerDto p = playersByName.get(tile.occupantName);
            String color = p != null ? p.color : "WHITE";
            int totemSize = (int) Math.round(40 * tileHeight / TILE_HEIGHT);
            TotemView totem = new TotemView(color, totemSize);

            double offsetProportion = 150.0 / actualH;
            double topMargin = tileHeight * offsetProportion - totemSize;
            javafx.geometry.Insets offset = new javafx.geometry.Insets(topMargin, 0, 0, 0);
            StackPane.setAlignment(totem, javafx.geometry.Pos.TOP_CENTER);
            StackPane.setMargin(totem, offset);
            getChildren().add(totem);
        }

        setMinSize(calculatedWidth, tileHeight);
        setPrefSize(calculatedWidth, tileHeight);

        if (clickable && tile.occupantName == null) {
            setStyle("-fx-cursor: hand;");
            setOnMouseClicked(e -> onClick.accept(tile.letter));
        }
    }
}
