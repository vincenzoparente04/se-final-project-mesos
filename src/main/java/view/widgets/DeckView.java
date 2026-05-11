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

    public static final double CARD_WIDTH = 90;
    public static final double CARD_HEIGHT = 130;
    private static final int STACK_SIZE = 4;
    private static final double OFFSET = 3.5;

    public DeckView(String backImageFilename) {
        String path = "/images/BackCards/" + backImageFilename;
        InputStream stream = backImageFilename != null
                ? getClass().getResourceAsStream(path) : null;

        Image img = stream != null ? new Image(stream) : null;

        for (int i = 0; i < STACK_SIZE; i++) {
            StackPane layer = new StackPane();
            layer.setTranslateX(i * OFFSET);
            layer.setTranslateY(-i * OFFSET);
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(CARD_WIDTH);
                iv.setFitHeight(CARD_HEIGHT);
                iv.setPreserveRatio(true);
                layer.getChildren().add(iv);
            } else {
                Rectangle r = new Rectangle(CARD_WIDTH, CARD_HEIGHT, Color.DARKSLATEGRAY);
                r.setStroke(Color.BLACK);
                r.setArcWidth(8);
                r.setArcHeight(8);
                layer.getChildren().add(r);
            }
            getChildren().add(layer);
        }

        double w = CARD_WIDTH + STACK_SIZE * OFFSET;
        double h = CARD_HEIGHT + STACK_SIZE * OFFSET;
        setMinSize(w, h);
        setPrefSize(w, h);
    }
}
