package view;

import javafx.scene.layout.StackPane;

/**
 * Contract for screen-level controllers managed by SceneRouter.
 * Every controller that SceneRouter loads must supply a root pane
 * (used for toast overlays and scene swaps) and accept a router binding.
 */
public interface SceneController extends ViewController {

    /**
     * Called by the {@link SceneRouter} in the loadFXML method to bind the controller and the router
     * in order to enable communication
     * @param router actual {@link SceneRouter}
     */
    void bind(SceneRouter router);

    /**
     * Called by the {@link SceneRouter}. Returns the root node of the controller's view.
     * @return the {@link StackPane} root of the splash screen
     */
    StackPane root();
}