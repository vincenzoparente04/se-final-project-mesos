package view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import shared.dto.CardDto;
import shared.dto.PlayerDto;
import view.widgets.CardView;

import java.io.IOException;

/**
 * Modal popup that shows another player's full tribe (characters + buildings).
 * Only opened from the table-side player markers, not from the player's own.
 */
public class TribePopupController {

    @FXML private Label titleLabel;
    @FXML private HBox charactersBox;
    @FXML private HBox buildingsBox;

    private Stage stage;

    public static void show(Window owner, PlayerDto target) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    TribePopupController.class.getResource("/org/example/mesos/tribe-popup.fxml"));
            Parent root = loader.load();
            TribePopupController ctrl = loader.getController();
            ctrl.init(target);

            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.initOwner(owner);
            st.setTitle(target.name + "'s tribe");
            st.setScene(new Scene(root));
            ctrl.stage = st;
            st.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load tribe-popup.fxml", e);
        }
    }

    private void init(PlayerDto target) {
        titleLabel.setText(target.name + "'s tribe");
        if (target.tribe == null) return;
        if (target.tribe.characterCards != null) {
            for (CardDto c : target.tribe.characterCards) {
                charactersBox.getChildren().add(new CardView(c, true, 80, 116));
            }
        }
        if (target.tribe.buildings != null) {
            for (CardDto b : target.tribe.buildings) {
                buildingsBox.getChildren().add(new CardView(b, true, 80, 116));
            }
        }
    }

    @FXML
    private void onClose() {
        if (stage != null) stage.close();
    }
}
