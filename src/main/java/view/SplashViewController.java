package view;

import javafx.fxml.FXML;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

/**
 * Static "MESOS" splash. Any click or key press advances to the connection form.
 */
public class SplashViewController {

    @FXML private StackPane rootPane;

    private SceneRouter router;

    public void bind(SceneRouter router) {
        this.router = router;
        rootPane.setOnMouseClicked(this::onAdvance);
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) oldScene.setOnKeyPressed(null);
            if (newScene != null) {
                newScene.setOnKeyPressed(this::onAdvance);
                rootPane.requestFocus();
            }
        });
    }

    private void onAdvance(MouseEvent e)   { router.toNick(); }
    private void onAdvance(KeyEvent e)     { router.toNick(); }
}
