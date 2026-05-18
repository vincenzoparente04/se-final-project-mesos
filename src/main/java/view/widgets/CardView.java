package view.widgets;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import shared.dto.CardDto;

import java.io.InputStream;

/**
 * Renders a single card (face up or face down) at a chosen size,
 * keeping the standard portrait aspect ratio of the source PNGs.
 */
public class CardView extends StackPane {

    /* TODO: ELIMINATE DEAD CODE
    public static final double DEFAULT_WIDTH  = 90;
    public static final double DEFAULT_HEIGHT = 130;

    public CardView(CardDto card, boolean faceUp) {
        this(card, faceUp, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
     */


    public CardView(CardDto card, boolean faceUp, double width, double height) {
        String filename = faceUp ? card.ImagePath : card.backImagePath;
        String dir = faceUp ? "FrontCards" : "BackCards";
        String path = "/images/" + dir + "/" + filename;

        Image img = filename != null ? ImageCache.get(path) : null;
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(width);
            iv.setFitHeight(height);
            iv.setPreserveRatio(true);
            getChildren().add(iv);
        } else {
            Rectangle r = new Rectangle(width, height, fallbackColor(card));
            r.setStroke(Color.GRAY);
            r.setArcWidth(8);
            r.setArcHeight(8);
            getChildren().add(r);
        }

        setMinSize(width, height);
        setPrefSize(width, height);
    }

    private static Color fallbackColor(CardDto card) {
        if (card == null || card.type == null) return Color.LIGHTGRAY;
        return switch (card.type) {
            case "CHARACTER" -> Color.LIGHTBLUE;
            case "EVENT" -> Color.LIGHTCORAL;
            case "BUILDING" -> Color.LIGHTYELLOW;
            default -> Color.LIGHTGRAY;
        };
    }
}
