package view.widgets;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.Optional;

public class CreateLobbyDialog extends Stage {

    private Integer chosen;

    public CreateLobbyDialog() {
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setResizable(false);

        Label title = new Label("New Lobby");
        title.getStyleClass().add("mesos-title-small");

        Label subtitle = new Label("How many players?");
        subtitle.getStyleClass().add("mesos-hint");

        Spinner<Integer> spinner = new Spinner<>(2, 5, 4);
        spinner.setEditable(false);
        spinner.setPrefWidth(160);
        spinner.getStyleClass().add("mesos-spinner");

        Button ok = new Button("Create");
        ok.setDefaultButton(true);
        ok.getStyleClass().add("mesos-button");
        ok.setOnAction(e -> { chosen = spinner.getValue(); close(); });

        Button cancel = new Button("Cancel");
        cancel.setCancelButton(true);
        cancel.getStyleClass().add("mesos-button-secondary");
        cancel.setOnAction(e -> close());

        HBox buttons = new HBox(12, ok, cancel);
        buttons.setAlignment(Pos.CENTER);

        VBox panel = new VBox(20, title, subtitle, spinner, buttons);
        panel.setAlignment(Pos.CENTER);
        panel.getStyleClass().add("mesos-dialog-panel");
        panel.setMaxWidth(320);
        panel.setMaxHeight(260);

        // drag support (no title bar)
        final double[] dragDelta = new double[2];
        panel.setOnMousePressed(e -> { dragDelta[0] = getX() - e.getScreenX(); dragDelta[1] = getY() - e.getScreenY(); });
        panel.setOnMouseDragged(e -> { setX(e.getScreenX() + dragDelta[0]); setY(e.getScreenY() + dragDelta[1]); });

        StackPane root = new StackPane(panel);
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root, 360, 280);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/styles/mesos.css").toExternalForm());
        setScene(scene);
    }

    public Optional<Integer> showAndWait(Window owner) {
        initOwner(owner);
        super.showAndWait();
        return Optional.ofNullable(chosen);
    }
}
