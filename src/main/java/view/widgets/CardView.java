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

    /**
     * Constructor method used to build the graphics of the card. It interrogates the {@link ImageCache}
     * to see if the image has already been loaded or else it loads it. Then it build the cardView.
     * @param card the DTO of the card
     * @param faceUp boolean to see if the card is faceUp or face Down
     * @param width dimension of the card
     * @param height dimension of the card
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

            double scale = Math.min(width / img.getWidth(), height / img.getHeight());
            double clipW = img.getWidth()  * scale;
            double clipH = img.getHeight() * scale;

            Rectangle clip = new Rectangle(clipW, clipH);
            clip.setArcWidth(36);
            clip.setArcHeight(36);
            iv.setClip(clip);

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

    /**
     * Helper if the card image is not found.
     * @param card the card to be substituted with
     * @return a static color
     */
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
