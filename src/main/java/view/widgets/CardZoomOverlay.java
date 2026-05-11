package view.widgets;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import shared.dto.CardDto;

/**
 * Translucent overlay that shows a single card at large size on top of the current scene.
 * Click anywhere on the backdrop (or the card itself) to dismiss.
 */
public final class CardZoomOverlay {

    private CardZoomOverlay() {}

    public static void show(StackPane root, CardDto card) {
        if (root == null || card == null) return;

        StackPane backdrop = new StackPane();
        backdrop.getStyleClass().add("mesos-card-zoom-backdrop");
        backdrop.setUserData("card-zoom-overlay");

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("mesos-card-zoom-detail");

        CardView big = new CardView(card, true, 280, 396);
        content.getChildren().add(big);

        Label type = new Label(formatType(card));
        type.getStyleClass().add("mesos-label-bold");
        content.getChildren().add(type);

        String meta = buildMetaText(card);
        if (meta != null && !meta.isBlank()) {
            Label metaLbl = new Label(meta);
            metaLbl.getStyleClass().add("mesos-card-meta");
            metaLbl.setWrapText(true);
            metaLbl.setMaxWidth(320);
            content.getChildren().add(metaLbl);
        }

        backdrop.getChildren().add(content);
        backdrop.setOnMouseClicked(e -> root.getChildren().remove(backdrop));
        root.getChildren().add(backdrop);
    }

    private static String formatType(CardDto c) {
        if (c.type == null) return "";
        return switch (c.type) {
            case "HUNTER"   -> "Hunter";
            case "BUILDER"  -> "Builder";
            case "SHAMAN"   -> "Shaman";
            case "ARTIST"   -> "Artist";
            case "INVENTOR" -> "Inventor";
            case "GATHERER" -> "Gatherer";
            case "EVENT"    -> "Event";
            case "BUILDING" -> "Building";
            default         -> c.type;
        };
    }

    private static String buildMetaText(CardDto c) {
        StringBuilder sb = new StringBuilder();
        if (c.details != null && !c.details.isBlank()) sb.append(c.details);
        if (c.foodCost > 0) {
            if (sb.length() > 0) sb.append("   •   ");
            sb.append("food cost: ").append(c.foodCost);
        }
        if (c.endGamePoints > 0) {
            if (sb.length() > 0) sb.append("   •   ");
            sb.append("end-game: ").append(c.endGamePoints).append(" PP");
        }
        if (c.era != null) {
            if (sb.length() > 0) sb.append("   •   ");
            sb.append("Era ").append(c.era);
        }
        return sb.toString();
    }
}
