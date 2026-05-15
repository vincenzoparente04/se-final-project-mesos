package view;

import javafx.fxml.FXML;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

/**
 * Static "MESOS" splash. Any click or key press advances to the connection form.
 */
public class SplashViewController implements SceneController {

    @FXML private StackPane rootPane;

    private SceneRouter router;

    @Override
    public StackPane root() { return rootPane; }

    @Override
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
