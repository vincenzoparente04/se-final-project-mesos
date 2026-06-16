package view.widgets;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;

/**
 * A "real" looking deck: several card-back images stacked with a small xy offset,
 * so it reads as a pile of cards on the table.
 * <p>
 * For tribe deck pass {@code "BackEra<n>.png"}; for building deck pass
 * {@code "BackBuildingEra<n>.png"} (or whatever filename the era resolves to).
 */
public class DeckView extends StackPane {

    /**Card width*/
    private static final double CARD_WIDTH = 90;
    /**Number of cards*/
    private static final int STACK_SIZE = 4;
    /**Offset between cards*/
    private static final double OFFSET = 3.5;


    /**
     * Constructor method of the deck view on the right of the board.
     * @param backImageFilename path to the back of the image.
     * @param cardWidth dimension of the card
     * @param cardHeight dimension of the card
     */
    public DeckView(String backImageFilename, double cardWidth, double cardHeight) {
        String path = "/images/BackCards/" + backImageFilename;
        Image img = backImageFilename != null ? ImageCache.get(path) : null;

        double offset = OFFSET * (cardWidth / CARD_WIDTH);

        for (int i = 0; i < STACK_SIZE; i++) {
            StackPane layer = new StackPane();
            layer.setTranslateX(i * offset);
            layer.setTranslateY(-i * offset);
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(cardWidth);
                iv.setFitHeight(cardHeight);
                iv.setPreserveRatio(true);
                Rectangle clip = new Rectangle(cardWidth, cardHeight);
                clip.setArcWidth(36);
                clip.setArcHeight(36);
                iv.setClip(clip);
                layer.getChildren().add(iv);
            } else {
                Rectangle r = new Rectangle(cardWidth, cardHeight, Color.DARKSLATEGRAY);
                r.setStroke(Color.BLACK);
                r.setArcWidth(8);
                r.setArcHeight(8);
                layer.getChildren().add(r);
            }
            getChildren().add(layer);
        }

        double w = cardWidth + STACK_SIZE * offset;
        double h = cardHeight + STACK_SIZE * offset;
        setMinSize(w, h);
        setPrefSize(w, h);
    }
}
