package view.widgets;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Tiny modal dialog asking how many players the new lobby should allow (2–5).
 * {@link #showAndWait(Window)} returns the chosen number, or {@code Optional.empty()}
 * if the user closed/cancelled the dialog.
 */
public class CreateLobbyDialog extends Stage {

    private Integer chosen;

    public CreateLobbyDialog() {
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Create a new lobby");
        setResizable(false);

        Label title = new Label("How many players?");
        title.getStyleClass().add("mesos-label-bold");

        Spinner<Integer> spinner = new Spinner<>(2, 5, 4);
        spinner.setEditable(false);
        spinner.setPrefWidth(120);

        Button ok = new Button("Create");
        ok.setDefaultButton(true);
        ok.getStyleClass().add("mesos-button");
        ok.setOnAction(e -> {
            chosen = spinner.getValue();
            close();
        });

        Button cancel = new Button("Cancel");
        cancel.setCancelButton(true);
        cancel.getStyleClass().add("mesos-button-secondary");
        cancel.setOnAction(e -> close());

        HBox buttons = new HBox(10, ok, cancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(14, title, spinner, buttons);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER_LEFT);

        Scene scene = new Scene(layout, 300, 170);
        scene.getStylesheets().add(
                getClass().getResource("/styles/mesos.css").toExternalForm());
        setScene(scene);
    }

    public Optional<Integer> showAndWait(Window owner) {
        initOwner(owner);
        super.showAndWait();
        return Optional.ofNullable(chosen);
    }
}
