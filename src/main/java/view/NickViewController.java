package view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import view.widgets.ErrorToast;

/**
 * Second connection screen. Collects a nickname and tries to register it with the server.
 * On failure, it shows the registration error.
 * On success, the {@link SceneRouter} moves the screen view forward to the lobby view.
 */
public class NickViewController implements SceneController {

    /** The root pane of the nickname screen layout. */
    @FXML private StackPane rootPane;
    /** Input field to collect the user's name*/
    @FXML private TextField nameField;
    /** Button to register the name*/
    @FXML private Button joinButton;
    /** Label to report change in status and errors */
    @FXML private Label statusLabel;

    /** The router used to navigate between scenes. */
    private SceneRouter router;

    /**
     * Returns the root node of this controller's view.
     */
    @Override
    public StackPane root() { return rootPane; }

    /**
     * Binds the scene router with this controller
     * @param router actual {@link SceneRouter}
     */
    @Override
    public void bind(SceneRouter router) {
        this.router = router;
    }

    /**
     * Called when the {@code joinButton} is clicked.
     * Checks the length of the inserted name (0 < name lenght < 21), disable the button and asks the {@link SceneRouter}
     * to set the name.
     */
    @FXML
    private void onJoin() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) { setStatus("Please enter a nickname.", true); return; }
        if (name.length() > 20) { setStatus("Please enter a shorter nickname.", true); return; }

        joinButton.setDisable(true);
        setStatus("Joining…", false);
        router.setName(name, this);
    }

    /**
     * Called by the {@link SceneRouter} when the name registration fails. Shows the error message and re-enables the join button.
     */
    public void onNickRejected() {
        Platform.runLater(() -> {
            setStatus("Nickname already taken.", true);
            ErrorToast.show(rootPane, "Nickname already taken");
            joinButton.setDisable(false);
        });
    }

    /**
     * Changes the statusLabel color and text.
     * @param msg status message
     * @param error boolean to change the text color in red if the message is an error
     */
    private void setStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle(error
                ? "-fx-text-fill: #e74c3c; -fx-font-size: 12;"
                : "-fx-text-fill: #f39c12; -fx-font-size: 12;");
    }
}
