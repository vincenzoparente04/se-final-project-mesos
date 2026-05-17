package view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import view.widgets.ErrorToast;

/**
 * Second connection screen. Collects a nickname then calls setName().
 */
public class NickViewController implements SceneController {

    @FXML private StackPane rootPane;
    @FXML private TextField nameField;
    @FXML private Button joinButton;
    @FXML private Label statusLabel;

    private SceneRouter router;

    @Override
    public StackPane root() { return rootPane; }

    @Override
    public void bind(SceneRouter router) {
        this.router = router;
    }

    @FXML
    private void onJoin() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) { setStatus("Please enter a nickname.", true); return; }
        if (name.length() > 20) { setStatus("Please enter a shorter nickname.", true); return; }

        joinButton.setDisable(true);
        setStatus("Joining…", false);
        router.setName(name, this);
    }

    public void onNickRejected() {
        Platform.runLater(() -> {
            setStatus("Nickname already taken.", true);
            ErrorToast.show(rootPane, "Nickname already taken");
            joinButton.setDisable(false);
        });
    }

    private void setStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle(error
                ? "-fx-text-fill: #e74c3c; -fx-font-size: 12;"
                : "-fx-text-fill: #f39c12; -fx-font-size: 12;");
    }
}
