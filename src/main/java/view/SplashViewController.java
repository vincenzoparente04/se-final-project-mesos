package view;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

/**
 * Static "MESOS" splash. Any click or key press advances to the connection form.
 */
public class SplashViewController implements SceneController {

    /** The root pane of the splash screen layout. */
    @FXML private StackPane rootPane;

    /** The router used to navigate between scenes. */
    private SceneRouter router;

    /**
     * Returns the root node of this controller's view.
     *
     * @return the {@link StackPane} root of the splash screen
     */
    @Override
    public StackPane root() { return rootPane; }

    /**
     * Binds the scene router to this controller and initializes input listeners.
     * Any mouse click or key press on the root pane will trigger an advance to the next screen.
     *
     * @param router actual {@link SceneRouter}
     */
    @Override
    public void bind(SceneRouter router) {
        this.router = router;
        rootPane.setOnMouseClicked(this::onAdvance);
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            //clean memory
            if (oldScene != null) oldScene.setOnKeyPressed(null);
            
            if (newScene != null) {
                newScene.setOnKeyPressed(this::onAdvance);
                rootPane.requestFocus();
            }
        });
    }

    /**
     * Asks the router to change scene after a mouse click
     * @param e
     */
    private void onAdvance(MouseEvent e) { router.toNetworkSetup(); }

    /**
     * Asks the router to change scene after a key is pressed
     * @param e
     */
    private void onAdvance(KeyEvent e) { router.toNetworkSetup(); }
}
