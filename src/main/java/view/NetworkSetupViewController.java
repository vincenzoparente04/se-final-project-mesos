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
 * First connection screen. Collects host, port, and transport then tries to connect to the server.
 * On failure, it shows the connection error.
 * On success, the {@link SceneRouter} moves the screen view forward to the nickname choosing view.
 */
public class NetworkSetupViewController implements SceneController {

    /** The root pane of the network screen layout. */
    @FXML private StackPane rootPane;
    /** Input field for host ip*/
    @FXML private TextField hostField;
    /** Input field for server port*/
    @FXML private TextField portField;
    /** RadioButton to select RMI transport protocol*/
    @FXML private RadioButton rmiRadio;
    /** RadioButton to select RMI transport protocol*/
    @FXML private RadioButton socketRadio;
    /** Button to send the connection attempt */
    @FXML private Button connectButton;
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
     * Initializes the transport type radioButton (checkbox) to change the suggested portField to the normal one for transport
     * once clicked.
     */
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

    /**
     * Called when the connect button is clicked. Validates input and attempts to connect using the {@link SceneRouter}.
     * Change the statusLabel if there are input errors.
     */
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

    /**
     * Called by the {@link SceneRouter} when there is a connection error. Displays it with an errorToast and with the
     * statusLabel.
     * @param message server returned msg
     */
    public void onConnectionError(String message) {
        Platform.runLater(() -> {
            setStatus("Connection failed: " + message, true);
            ErrorToast.show(rootPane, "Connection failed");
            connectButton.setDisable(false);
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
