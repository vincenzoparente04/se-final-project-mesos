package view.widgets;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.InputStream;

/**
 * Visual marker for a player's totem.
 * Tries to load /images/Totems/Totem_<COLOR>.png; if missing, renders a coloured circle
 * so the UI keeps working before the assets are added.
 */
public class TotemView extends StackPane {

    public TotemView(String colorName, double size) {
        if (colorName == null) colorName = "WHITE";

        String path = "/images/totems/totem_" + colorName.toLowerCase() + ".png";
        Image img = ImageCache.get(path);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setPreserveRatio(true);
            getChildren().add(iv);
        } else {
            Circle c = new Circle(size / 2.0, paintFor(colorName));
            c.setStroke(Color.BLACK);
            c.setStrokeWidth(1);
            getChildren().add(c);
        }
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
    }

    private static Color paintFor(String colorName) {
        return switch (colorName) {
            case "RED" -> Color.web("#e74c3c");
            case "BLUE" -> Color.web("#3498db");
            case "GREEN" -> Color.web("#2ecc71");
            case "YELLOW" -> Color.web("#f1c40f");
            case "WHITE" -> Color.web("#ecf0f1");
            default -> Color.LIGHTGRAY;
        };
    }
}
