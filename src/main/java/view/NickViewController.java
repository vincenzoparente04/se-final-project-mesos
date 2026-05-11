package view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import network.client.ConnectionProtocol;
import network.client.VirtualServer;
import network.client.VirtualServerFactory;
import view.widgets.ErrorToast;

/**
 * Connection form. Spawns a background thread to build a VirtualServer;
 * the lobby screen is shown only after the connection succeeds.
 */
public class NickViewController {

    @FXML private StackPane rootPane;
    @FXML private TextField nameField;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private RadioButton rmiRadio;
    @FXML private RadioButton socketRadio;
    @FXML private Button connectButton;
    @FXML private Label statusLabel;

    private SceneRouter router;

    public void bind(SceneRouter router) {
        this.router = router;
        if (router.playerName() != null) nameField.setText(router.playerName());
    }

    @FXML
    private void onConnect() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String host = hostField.getText() == null ? "" : hostField.getText().trim();
        String portText = portField.getText() == null ? "" : portField.getText().trim();

        if (name.isEmpty()) { setStatus("Please enter a nickname.", true); return; }
        if (host.isEmpty()) { setStatus("Please enter a host.", true); return; }
        int port;
        try { port = Integer.parseInt(portText); }
        catch (NumberFormatException e) { setStatus("Port must be a number.", true); return; }

        ConnectionProtocol transport = rmiRadio.isSelected()
                ? ConnectionProtocol.RMI : ConnectionProtocol.SOCKET;

        connectButton.setDisable(true);
        setStatus("Connecting…", false);

        new Thread(() -> {
            try {
                VirtualServer proxy = VirtualServerFactory.create(
                        transport, host, port, name,
                        router.localState(), router.listener());
                Platform.runLater(() -> {
                    router.setPlayerName(name);
                    router.setVirtualServer(proxy);
                    router.toLobby();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setStatus("Connection failed: " + ex.getMessage(), true);
                    ErrorToast.show(rootPane, "Connection failed");
                    connectButton.setDisable(false);
                });
            }
        }, "connect-" + name).start();
    }

    private void setStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle(error
                ? "-fx-text-fill: #e74c3c; -fx-font-size: 12;"
                : "-fx-text-fill: #f39c12; -fx-font-size: 12;");
    }
}
