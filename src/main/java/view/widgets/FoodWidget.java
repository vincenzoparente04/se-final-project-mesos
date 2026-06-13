package view.widgets;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.io.InputStream;

/** Food icon + amount label. */
public class FoodWidget extends HBox {

    /**main method of the food widget, build boop bap*/
    public FoodWidget(int amount) {
        super(3);
        setAlignment(Pos.CENTER);

        Image img = ImageCache.get("/images/icons/food.png");
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(16);
            iv.setFitHeight(16);
            iv.setPreserveRatio(true);
            getChildren().add(iv);
        }
        Label l = new Label(String.valueOf(amount));
        l.getStyleClass().add("mesos-player-food");
        getChildren().add(l);
    }
}
