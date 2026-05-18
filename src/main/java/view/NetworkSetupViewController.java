package view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import view.widgets.ErrorToast;

/**
 * First connection screen. Collects host, port, and transport then calls connect().
 */
public class NetworkSetupViewController implements SceneController {

    @FXML private StackPane rootPane;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private RadioButton rmiRadio;
    @FXML private RadioButton socketRadio;
    @FXML private Button connectButton;
    @FXML private Label statusLabel;

    private SceneRouter router;

    @Override
    public StackPane root() { return rootPane; }

    @Override
    public void bind(SceneRouter router) {
        this.router = router;
    }

    @FXML
    public void initialize() {
        rmiRadio.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
            if (isNowSelected) {
                portField.setText("1099");
            }
        });
        socketRadio.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
            if (isNowSelected) {
                portField.setText("9999");
            }
        });
    }

    @FXML
    private void onConnect() {
        String host = hostField.getText() == null ? "" : hostField.getText().trim();
        String portText = portField.getText() == null ? "" : portField.getText().trim();

        if (host.isEmpty()) { setStatus("Please enter a host.", true); return; }
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            setStatus("Port must be a number.", true); return;
        }

        String transport = rmiRadio.isSelected() ? "RMI" : "SOCKET";

        connectButton.setDisable(true);
        setStatus("Connecting…", false);

        router.connect(transport, host, port, this);
    }

    public void onConnectionError(String message) {
        Platform.runLater(() -> {
            setStatus("Connection failed: " + message, true);
            ErrorToast.show(rootPane, "Connection failed");
            connectButton.setDisable(false);
        });
    }

    private void setStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle(error
                ? "-fx-text-fill: #e74c3c; -fx-font-size: 12;"
                : "-fx-text-fill: #f39c12; -fx-font-size: 12;");
    }
}
