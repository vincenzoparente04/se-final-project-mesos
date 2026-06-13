package view.widgets;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.InputStream;

/** Prestige-points crown with the amount overlaid at centre. */
public class PpWidget extends StackPane {

    /**size of the widget*/
    private static final double SIZE = 38;
    /**offset of the pp of the widget*/
    private static final double LABEL_OFFSET_Y = -4;

    /**main class of the ppWidget, get images does all sort of things bee boop*/
    public PpWidget(int amount) {
        Image img = ImageCache.get("/images/icons/PuntiPrestige.png");
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(SIZE);
            iv.setFitHeight(SIZE);
            iv.setPreserveRatio(true);
            getChildren().add(iv);
        }
        Label l = new Label(String.valueOf(amount));
        l.getStyleClass().add("mesos-player-pp");
        l.setTranslateY(LABEL_OFFSET_Y);
        StackPane.setAlignment(l, Pos.CENTER);
        getChildren().add(l);
    }
}
