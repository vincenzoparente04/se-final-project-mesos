package view;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import network.client.ClientController;
import network.client.LocalGameState;
import shared.dto.CardDto;
import shared.dto.OfferTileDto;
import shared.dto.PlayerDto;
import shared.dto.TurnOrderSlotDto;

import java.io.InputStream;
import java.util.List;


public class GameViewController {

    // ─── FXML fields ────────────────────────────────────────────────────────

    @FXML private StackPane rootPane;

    @FXML private Label phaseLabel;
    @FXML private Label roundLabel;
    @FXML private Label eraLabel;
    @FXML private Label currentPlayerLabel;

    @FXML private VBox playersPanel;

    @FXML private VBox  colorChoiceBox;
    @FXML private HBox  colorButtonsBox;

    @FXML private HBox offerTrackBox;
    @FXML private HBox turnOrderBox;

    @FXML private HBox topTribeBox;
    @FXML private HBox bottomTribeBox;
    @FXML private HBox topBuildingBox;
    @FXML private HBox bottomBuildingBox;

    @FXML private Label  actionInfoLabel;
    @FXML private Button endTurnButton;

    @FXML private StackPane gameOverPane;
    @FXML private Label     winnersLabel;

    // ─── State ──────────────────────────────────────────────────────────────

    private ClientController clientController;
    private String myPlayerName;
    private boolean colorButtonsBuilt = false;

    // ─── Wiring (called by ClientStateListenerGui) ───────────────────────────

    public void setClientController(ClientController cc) {
        this.clientController = cc;
    }

    public void setMyPlayerName(String name) {
        this.myPlayerName = name;
    }

    // ─── Main update entry-point ─────────────────────────────────────────────

    /**
     * Refreshes the entire scene from a fresh LocalGameState snapshot.
     * Must be called on the JavaFX Application Thread (via Platform.runLater).
     */
    public void update(LocalGameState state) {
        String phase = state.getPhase();
        boolean isMyTurn = myPlayerName != null && myPlayerName.equals(state.getCurrentPlayerName());

        updateHeader(state, phase);
        updateColorChoicePanel(phase, isMyTurn);
        updateOfferTrack(state.getOfferTiles(), phase, isMyTurn);
        updateTurnOrder(state.getTurnOrderSlots());
        updateCardRows(state, phase, isMyTurn);
        updatePlayersPanel(state.getPlayers());
        updateActionBar(phase, isMyTurn, state.getCurrentPlayerName());

        if (state.isGameOver() && !state.getWinners().isEmpty()) {
            showGameOver(state.getWinners());
        }
    }

    public void showGameOver(List<String> winners) {
        winnersLabel.setText("Winners: " + String.join(", ", winners));
        gameOverPane.setVisible(true);
        // Make the overlay fill the root StackPane
        gameOverPane.setPrefSize(rootPane.getWidth(), rootPane.getHeight());
    }

    // ─── FXML handlers ──────────────────────────────────────────────────────

    @FXML
    private void onEndTurn() {
        if (clientController != null) clientController.onTurnEnded();
    }

    // ─── Private update methods ──────────────────────────────────────────────

    private void updateHeader(LocalGameState state, String phase) {
        phaseLabel.setText(formatPhase(phase));
        roundLabel.setText("Round " + state.getCurrentRound() + "/10");
        eraLabel.setText(state.getCurrentEra() != null ? state.getCurrentEra() : "");
        String cp = state.getCurrentPlayerName();
        currentPlayerLabel.setText(cp != null ? cp : "—");
    }

    private void updateColorChoicePanel(String phase, boolean isMyTurn) {
        boolean show = "COLOR_CHOOSING_PHASE".equals(phase) && isMyTurn;
        colorChoiceBox.setVisible(show);
        colorChoiceBox.setManaged(show);

        if (show && !colorButtonsBuilt) {
            buildColorButtons();
            colorButtonsBuilt = true;
        }
    }

    //TODO: restyle it, now it's a mess, specifically the String[][] colors
    private void buildColorButtons() {
        String[][] colors = {
                {"RED",    "Red",    "#ff5c5c"},
                {"BLUE",   "Blue",   "#5da9ff"},
                {"GREEN",  "Green",  "#58d68d"},
                {"YELLOW", "Yellow", "#f7dc6f"},
                {"WHITE",  "White",  "#ffffff"}
        };

        colorButtonsBox.getChildren().clear();

        for (String[] entry : colors) {
            String colorName  = entry[0];
            String colorLabel = entry[1];
            String hexColor   = entry[2];

            Button btn = new Button(colorLabel);

            String textColor = (colorName.equals("WHITE") || colorName.equals("YELLOW")) ? "#333333" : "white";

            btn.setStyle(
                    "-fx-background-color: " + hexColor + ";" +
                            "-fx-text-fill: " + textColor + ";" +
                            "-fx-font-size: 10px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-min-width: 45px; -fx-min-height: 45px;" +
                            "-fx-max-width: 45px; -fx-max-height: 45px;" +
                            "-fx-cursor: hand;" +
                            "-fx-border-color: rgba(0,0,0,0.1); -fx-border-width: 1;"
            );

            btn.setOnAction(e -> {
                if (clientController != null) clientController.onColorChosen(colorName);
            });

            colorButtonsBox.getChildren().add(btn);
        }
    }

    private void updateOfferTrack(List<OfferTileDto> tiles, String phase, boolean isMyTurn) {
        offerTrackBox.getChildren().clear();
        boolean canPlace = "PLACEMENT".equals(phase) && isMyTurn;
        for (OfferTileDto tile : tiles) {
            offerTrackBox.getChildren().add(buildOfferTileNode(tile, canPlace));
        }
    }

    private VBox buildOfferTileNode(OfferTileDto tile, boolean clickable) {
        boolean free = tile.occupantName == null;

        VBox box = new VBox(4);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-border-color: #7f8c8d; -fx-border-width: 1; -fx-background-color: "
                + (clickable && free ? "#d5f5e3" : "#f0f0f0") + ";");

        //TODO: remove stream and use image constructor with path directly
        String path = "/images/TileCards/OfferTrail_" + tile.letter + "_FRONT.png";
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream != null) {
            Image img = new Image(stream);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(60);
            iv.setPreserveRatio(true);
            box.getChildren().add(iv);
        } else {
            System.err.println("Could NOT load image layout fallback for: " + path);
        }

        Label letter  = new Label("Tile " + tile.letter);
        letter.setStyle("-fx-font-weight: bold;");

        String actionText = "DRAW_CARDS".equals(tile.actionType)
                ? "Draw (" + tile.topRowLimit + "/" + tile.bottomRowLimit + ")"
                : "Take food";
        Label action   = new Label(actionText);
        Label occupant = new Label(free ? "Free" : "↳ " + tile.occupantName); //
        occupant.setStyle(free ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: red;");

        box.getChildren().addAll(letter, action, occupant);

        if (clickable && free) {
            box.setStyle(box.getStyle() + " -fx-cursor: hand;");
            box.setOnMouseClicked(e -> {
                if (clientController != null) clientController.onTotemPlaced(tile.letter);
            });
        }
        return box;
    }

    private void updateTurnOrder(List<TurnOrderSlotDto> slots) {
        turnOrderBox.getChildren().clear();
        for (TurnOrderSlotDto slot : slots) {
            Label lbl = new Label((slot.position + 1) + ". "
                    + (slot.occupantName != null ? slot.occupantName : "—"));
            lbl.setPadding(new Insets(4, 10, 4, 10));
            lbl.setStyle("-fx-border-color: #aaaaaa; -fx-border-width: 1;");
            turnOrderBox.getChildren().add(lbl);
        }
    }

    private void updateCardRows(LocalGameState state, String phase, boolean isMyTurn) {
        boolean canDraw = "ACTION".equals(phase) && isMyTurn;
        renderCardRow(topTribeBox,     state.getTopRowTribe(),     canDraw);
        renderCardRow(bottomTribeBox,  state.getBottomRowTribe(),  canDraw);
        renderCardRow(topBuildingBox,  state.getTopRowBuilding(),  canDraw);
        renderCardRow(bottomBuildingBox, state.getBottomRowBuilding(), canDraw);
    }

    private void renderCardRow(HBox box, List<CardDto> cards, boolean clickable) {
        box.getChildren().clear();
        for (CardDto card : cards) {
            box.getChildren().add(buildCardNode(card, clickable));
        }
    }

    private VBox buildCardNode(CardDto card, boolean clickable) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(4));
        box.setPrefWidth(92);
        box.setStyle("-fx-border-width: 1; -fx-border-color: "
                + (clickable ? "#2980b9" : "#bdc3c7") + "; -fx-background-color: "
                + (clickable ? "#ebf5fb" : "#f9f9f9") + ";");

        //TODO: remove stream and use image constructor with path directly
        String path = "/images/FrontCards/" + card.ImagePath;
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream != null) {
            Image img = new Image(stream);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(80);
            iv.setFitHeight(106);
            iv.setPreserveRatio(true);
            box.getChildren().add(iv);
        } else {
            System.err.println("Could NOT load image layout fallback for: " + path + " - card type: " + card.type + " id: " + card.id);
            Rectangle rect = new Rectangle(80, 106, cardTypeColor(card.type));
            rect.setStroke(Color.GRAY);
            box.getChildren().add(rect);
        }

        Label typeLabel = new Label(formatCardType(card.type));
        typeLabel.setStyle("-fx-font-size: 9;");
        Label idLabel = new Label("ID " + card.id
                + (card.foodCost > 0 ? " Food: " + card.foodCost : "")
                + (card.endGamePoints > 0 ? "  Aura Points:" + card.endGamePoints : ""));
        idLabel.setStyle("-fx-font-size: 9;");
        box.getChildren().addAll(typeLabel, idLabel);

        if (clickable) {
            box.setStyle(box.getStyle() + " -fx-cursor: hand;");
            box.setOnMouseClicked(e -> {
                if (clientController != null) clientController.onCardDrawn(card.id);
            });
        }
        return box;
    }

    private void updatePlayersPanel(List<PlayerDto> players) {
        playersPanel.getChildren().clear();
        Label header = new Label("Players");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
        playersPanel.getChildren().add(header);

        for (PlayerDto p : players) {
            boolean isMe = p.name.equals(myPlayerName);
            VBox pBox = new VBox(2);
            pBox.setPadding(new Insets(6));
            pBox.setStyle("-fx-border-color: #dddddd; -fx-border-width: 1; "
                    + "-fx-background-color: " + (isMe ? "#d6eaf8" : "#fafafa") + ";");

            Label name = new Label(p.name + (isMe ? "  (tu)" : ""));
            name.setStyle("-fx-font-weight: bold;");
            Label stats  = new Label("Food: " + p.food + "   Aura Points: " + p.prestigePoints);
            Label color  = new Label("Color: " + p.color);
            Label totem  = new Label("Totem: " + formatTotemLocation(p.totemLocation));
            
            // Show acquired cards
            HBox acquiredCardsBox = new HBox(2);
            if (p.tribe != null) {
                if (p.tribe.characterCards != null) {
                    for (CardDto c : p.tribe.characterCards) {
                        addSmallCardIcon(acquiredCardsBox, c);
                    }
                }
                if (p.tribe.buildings != null) {
                    for (CardDto b : p.tribe.buildings) {
                        addSmallCardIcon(acquiredCardsBox, b);
                    }
                }
            }

            pBox.getChildren().addAll(name, stats, color, totem, acquiredCardsBox);
            playersPanel.getChildren().add(pBox);
        }
    }
    
    private void addSmallCardIcon(HBox container, CardDto c) {
        //TODO: remove stream and use image constructor with path directly
        String path = "/images/FrontCards/" + c.ImagePath;
        InputStream st = getClass().getResourceAsStream(path);
        if (st != null) {
            ImageView iv = new ImageView(new Image(st));
            iv.setFitHeight(28);
            iv.setPreserveRatio(true);
            container.getChildren().add(iv);
        } else {
            Rectangle rect = new Rectangle(20, 28, cardTypeColor(c.type));
            rect.setStroke(Color.GRAY);
            container.getChildren().add(rect);
        }
    }

    private void updateActionBar(String phase, boolean isMyTurn, String currentPlayer) {
        boolean showEndTurn = "ACTION".equals(phase);
        endTurnButton.setVisible(showEndTurn);
        endTurnButton.setManaged(showEndTurn);
        endTurnButton.setDisable(!isMyTurn);

        if (isMyTurn) {
            actionInfoLabel.setText("It's your turn!");
            actionInfoLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else if (currentPlayer != null) {
            actionInfoLabel.setText("Waiting " + currentPlayer + "...");
            actionInfoLabel.setStyle("-fx-text-fill: gray;");
        } else {
            actionInfoLabel.setText("");
        }
    }

    // ─── Formatters ──────────────────────────────────────────────────────────
    // TODO: determine whether the switch are ok
    private String formatPhase(String phase) {
        if (phase == null) return "—";
        return switch (phase) {
            case "SETUP"               -> "Setup";
            case "COLOR_CHOOSING_PHASE"-> "Choose color";
            case "PLACEMENT"           -> "Placement";
            case "ACTION"              -> "Action";
            case "PRE_END_OF_ROUND"    -> "Pre-end of round";
            case "END_OF_ROUND"        -> "End of round";
            case "END_OF_GAME"         -> "End of game";
            default                    -> phase;
        };
    }

    private String formatTotemLocation(String loc) {
        if (loc == null) return "—";
        return switch (loc) {
            case "TURN_ORDER_TILE" -> "Turn order";
            case "OFFER_TRACK"     -> "Offer track";
            default                -> loc;
        };
    }

    private String formatCardType(String type) {
        if (type == null) return "";
        return switch (type) {
            case "CHARACTER" -> "Character";
            case "EVENT"     -> "Event";
            case "BUILDING"  -> "Building";
            default          -> type;
        };
    }

    private Color cardTypeColor(String type) {
        if (type == null) return Color.LIGHTGRAY;
        return switch (type) {
            case "CHARACTER" -> Color.LIGHTBLUE;
            case "EVENT"     -> Color.LIGHTCORAL;
            case "BUILDING"  -> Color.LIGHTYELLOW;
            default          -> Color.LIGHTGRAY;
        };
    }
}
