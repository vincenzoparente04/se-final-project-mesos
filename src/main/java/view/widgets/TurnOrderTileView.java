package view.widgets;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import shared.dto.PlayerDto;
import shared.dto.TurnOrderSlotDto;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * The TurnOrderTile of the board, showing the n-player tile background
 * and any totems currently sitting on its slots.
 * The tile image is read as {@code Order_<n>_Players_FRONT.png}.
 */
public class TurnOrderTileView extends StackPane {

    public static final double TILE_WIDTH = 110 * (300.0/463.0); // True aspect ratio
    public static final double TILE_HEIGHT = 110;

    public TurnOrderTileView(List<TurnOrderSlotDto> slots,
                             Map<String, PlayerDto> playersByName) {
        int playerCount = slots.size();
        String path = "/images/TileCards/Order_" + playerCount + "_Players_FRONT.png";
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream != null) {
            ImageView iv = new ImageView(new Image(stream));
            iv.setFitHeight(TILE_HEIGHT);
            iv.setPreserveRatio(true);
            getChildren().add(iv);
        } else {
            Rectangle r = new Rectangle(TILE_WIDTH, TILE_HEIGHT, Color.web("#fdebd0"));
            r.setStroke(Color.web("#a04000"));
            getChildren().add(r);
        }

        /*
        VBox totems = new VBox(-10); // Negative spacing pushes them slightly together if needed, or adjust to match circles
        totems.setAlignment(Pos.TOP_CENTER);
        // Add padding to push the VBox down exactly to where the first circle starts on the image.
        // We will adjust the top padding (e.g. 15% of the total height)
        totems.setPadding(new javafx.geometry.Insets(TILE_HEIGHT * 0.15, 0, 0, 0)); 
        
        for (TurnOrderSlotDto slot : slots) {
            StackPane cell = new StackPane();
            // Match the height to the distance between circles on the card image
            cell.setPrefSize(TILE_WIDTH, TILE_HEIGHT / slots.size() * 0.7); 
            if (slot.occupantName != null) {
                PlayerDto p = playersByName.get(slot.occupantName);
                String color = p != null ? p.color : "WHITE";
                cell.getChildren().add(new TotemView(color, 20)); // Size the totem to fit the circle
            }
            totems.getChildren().add(cell);
        }
        getChildren().add(totems);

         */

        // Usa un Pane di base invece di un VBox per permettere il posizionamento assoluto
        Pane totemLayer = new Pane();
        totemLayer.setPrefSize(TILE_WIDTH, TILE_HEIGHT);

        int numPlayers = slots.size();
        double[] yOffsets;

        // 1. Definisci le coordinate Y di partenza per ogni carta (valori da calibrare)
        switch (numPlayers) {
            case 2:
                yOffsets = new double[]{145.0 / 462.0, 222.0 / 462.0};
                break;
            case 3:
                yOffsets = new double[]{122.0/462.0, 202.0/462.0, 282.0/462.0};
                break;
            case 4:
                yOffsets = new double[]{104.0/462.0, 184.0/462.0 , 262.0/462.0 , 341.0/462.0};
                break;
            case 5:
                yOffsets = new double[]{74.0/462.0, 151.0/462.0, 231.0/462.0 , 312.0/ 462.0, 390.0/ 462.0};
                break;
            default:
                yOffsets = new double[numPlayers];
        }

        // Stima dell'altezza della singola casella bianca (circa il 15% della carta)
        double boxHeight = TILE_HEIGHT * 0.15;

        double totemSize = 23.0+15.0;
        double totemHeight = totemSize;
        // 2. Posiziona i Totem
        for (int i = 0; i < slots.size(); i++) {
            TurnOrderSlotDto slot = slots.get(i);

            if (slot.occupantName != null) {
                PlayerDto p = playersByName.get(slot.occupantName);
                String color = p != null ? p.color : "WHITE";
                TotemView totem = new TotemView(color, 40);

                // Usiamo uno StackPane grande quanto la carta in larghezza e quanto la casella in altezza
                // Questo centra automaticamente il totem al centro della casellina sull'asse X e Y
                StackPane cell = new StackPane(totem);

                cell.setPrefHeight(boxHeight);
                cell.setPrefWidth(TILE_WIDTH);
                cell.setMinWidth(TILE_WIDTH);
                cell.setMaxWidth(TILE_WIDTH);

                cell.setLayoutX(0);
                cell.setAlignment(Pos.BOTTOM_CENTER);

                //StackPane.setAlignment(totem, Pos.BOTTOM_CENTER);

                double pavimentoY = TILE_HEIGHT * yOffsets[i];
                // Impostiamo l'esatta posizione verticale per questa specifica casella
                cell.setLayoutY(pavimentoY - totemHeight); // Posiziona il totem in modo che "tocchi" il pavimento

                totemLayer.getChildren().add(cell);
            }
        }

        // Aggiungi il layer al genitore principale
        getChildren().add(totemLayer);

        setMinSize(TILE_WIDTH, TILE_HEIGHT);
        setPrefSize(TILE_WIDTH, TILE_HEIGHT);
    }
}
