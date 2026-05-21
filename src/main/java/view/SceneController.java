package view;

import javafx.scene.layout.StackPane;

/**
 * Contract for screen-level controllers managed by SceneRouter.
 * Every controller that SceneRouter loads must supply a root pane
 * (used for toast overlays and scene swaps) and accept a router binding.
 */
public interface SceneController extends ViewController {

    void bind(SceneRouter router);

    StackPane root();
}
