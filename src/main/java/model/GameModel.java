package model;

import javafx.beans.Observable;
import model.board.Board;
import model.board.OfferTile;
import model.board.OfferTileAction;
import model.board.TurnOrderSlot;
import model.cards.BuildingCard;
import model.cards.Card;
import model.cards.CharacterCard;
import model.cards.EventCard;
import model.deck.BuildingDeck;
import model.deck.TribeDeck;
import model.enums.*;
import model.player.Player;
import model.player.Totem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static model.enums.TotemLocation.TURN_ORDER_TILE;

public class GameModel extends Observable {

    private Board board;
    private TribeDeck tribeDeck;
    private BuildingDeck buildingDeckEraI;
    private BuildingDeck buildingDeckEraII;
    private BuildingDeck buildingDeckEraIII;
    private List<Player> players;
    private int playerCount;
    private Player currentPlayer;
    private int currentPlayerIndex;
    private int cardsDrawnFromTopRow;
    private int cardsDrawnFromBottomRow;

    private GamePhase currentPhase;
    private GameState gameState;
    private int currentRound;
    private Era currentEra;

    // color choosing phase
    private Set<TotemColor> availableColors;
    private int colorChoosingPlayerIndex;
    private Player colorChoosingPlayer;

    // setup phase -----------------------------------------------------------------------------------------------------
    public void startGame(List<String> playerNames){
        createPlayers(playerNames);
        initializeColorChoosing();
        // Players will now send their color choices via chooseColor(player, color)
        // Once all have chosen, completeSetup() will be automatically called
    }

    /**
     * @param playerNames
     * @implNote Creates Player objects for each player name provided and adds them to the players list.
     * This is called during startGame() to initialize the player list before the color choosing phase.
     */
    private void createPlayers(List<String> playerNames){
        this.players = new ArrayList<>();
        for (String name : playerNames) {
            this.players.add(new Player(name));
            this.playerCount++;
        }
    }


    /**
     * Initialize the color choosing phase.
     * Called from startGame() before actual game setup.
     */
    private void initializeColorChoosing() {
        // Create a mutable set of all available colors (all enum values)
        availableColors = EnumSet.allOf(TotemColor.class);

        // Start with the first player
        colorChoosingPlayerIndex = 0;
        colorChoosingPlayer = players.get(colorChoosingPlayerIndex);

        // Notify that color choosing phase has started
        notifyChange("color_choosing_started:" + colorChoosingPlayer.getName());
    }

    /**
     * Called by the Controller when a Player sends their color choice through client-server communication.
     * Validates the choice, assigns the totem color, and advances to the next player.
     *
     * @param player The player making the choice
     * @param color The totem color chosen by the player
     * @throws IllegalStateException if it's not the player's turn to choose or color is unavailable
     */
    public void chooseColor(Player player, TotemColor color) throws IllegalStateException {
        // Validate: is it this player's turn to choose?
        if (player != colorChoosingPlayer) {
            throw new IllegalStateException(
                    "It's not " + player.getName() + "'s turn to choose a color. " +
                            "Waiting for " + colorChoosingPlayer.getName()
            );
        }

        // Validate: is the color available?
        if (!availableColors.contains(color)) {
            throw new IllegalStateException(
                    "Color " + color + " is not available. Available colors: " + availableColors
            );
        }

        // Assign the totem to the player with the chosen color
        player.setTotem(new Totem(player, color));

        // Remove the color from available pool
        availableColors.remove(color);

        // Notify observers about the color assignment
        notifyChange("color_chosen:" + player.getName() + ":" + color);

        // Advance to next player's color choice or start the game
        advanceColorChoosingTurn();
    }


    /**
     * Advance to the next player's turn to choose a color.
     * When all players have chosen, proceed with game setup.
     */
    private void advanceColorChoosingTurn() {
        colorChoosingPlayerIndex++;

        // Check if there are more players to choose
        if (colorChoosingPlayerIndex < players.size()) {
            colorChoosingPlayer = players.get(colorChoosingPlayerIndex);
            notifyChange("color_choosing_next:" + colorChoosingPlayer.getName());
        } else {
            // All players have chosen their colors - now complete the setup
            notifyChange("color_choosing_completed");
            completeSetup();
        }
    }

    /**
     * Complete the game setup after all players have chosen their colors.
     * This initializes all decks, the board, and starts the game.
     */
    private void completeSetup() {
        tribeDeck.initializeDeck(playerCount);
        buildingDeckEraI.initializeDeck(playerCount);
        buildingDeckEraII.initializeDeck(playerCount);
        buildingDeckEraIII.initializeDeck(playerCount);
        board.setupBoard(tribeDeck, buildingDeckEraI, playerCount);
        randomizeTurnOrder();
        distributeFood();
        setPhase(GamePhase.PLACEMENT);
    }


    private void distributeFood(){
        player.addFood() // per ogni giocatore
    }
    private void randomizeTurnOrder(){
        board.getTurnOrderTile().placeTotemAtSlot(player.getTotem(), index);
        player.getTotem.setLocation();
        // chiamerà:
        // turnOrderTile.placeTotemAtSlot();
        // player.totem.setLocation()
        // TODO da implementare
    }

    // PLACEMENT PHASE -------------------------------------------------------------------------------------------------

    //TODO: -decide if we're gonna recive the objects player and offerTile from the controller or if we're gonna find them by their IDs here or in the subclasses
    //TODO: -decide how to handle the exceptions, how to call them
    /**
     * @implNote Validate the placeTotemAction, then delegates to the board the placeTotem implementation. At the end notifies the changes and updates the placement turn phase.
     * @param player
     * @param offerTile
     */
    public void placeTotem(Player player, OfferTile offerTile){

        //validate the move
        try {
            canPlaceTotem(player, offerTile);
        }catch(Exception e){
            //how to handle the exceptions?
        }

        //place the totem on the offer tile
        try{
            board.placeTotem(player.getTotem(), offerTile);
        }catch(Exception e){
            //how to handle the exception?
        }

        //notifies change
        notifyChange("totem_placed");

        //update the placement turn phase
        advancePlacementTurn();
    }

    public boolean canPlaceTotem(Player player, OfferTile tile) throws Exception{
        // Controlla:
        //player == currentPlayer          // è il turno di questo giocatore?
        //currentPhase == PLACEMENT        // siamo nella fase giusta?
        //tile.isOccupied()                // la casella è libera?
        //player.getTotem().getLocation()  // il Totem è disponibile?
        //    == TotemLocation.TURN_ORDER_TILE

        //player is currentPlayer
        if(player != currentPlayer){
            throw new Exception("It's not your turn!");
        }

        //currentPhase== PLACEMENT;
        if(currentPhase != GamePhase.PLACEMENT){
            throw new Exception("It's not the placement phase!");
        }

        //player.getTotem().getLocation() == TotemLocation.TURN_ORDER_TILE;
        if(currentPlayer.getTotem().getLocation() != TotemLocation.TURN_ORDER_TILE){
            throw new Exception("Your totem is already on the offer track");
        }

    }

    private void advancePlacementTurn(){
        List<Player> order = board.getTurnOrderTile().getTurnOrder();
        currentPlayerIndex++;

        if(currentPlayerIndex < order.size()){
            currentPlayer = order.get(currentPlayerIndex);
            notifyChange("turn_changed");
        }
        if(currentPlayerIndex >= order.size()){
            setPhase(GamePhase.ACTION);
            currentPlayerIndex = 0;
            currentPlayer = getFirstPlayerInActionPhase();
            notifyChange("phase_changed")
        }
    }
    private Player getFirstPlayerInActionPhase(){
        return board.getOfferTrack().getOccupiedTilesInOrder().get(0).getOccupant().getOwner();
    }

    // ACTION PHASE ----------------------------------------------------------------------------------------------------

    public void drawCard(int cardId) {
        if (!canDrawCard(cardId)) return;

        // aggiorna il contatore della riga corretta
        if (board.getTopRow().containsCard(cardId)) {
            cardsDrawnFromTopRow++;
        } else {
            cardsDrawnFromBottomRow++;
        }

        // rimuove la carta dal tabellone
        Card card = board.findCardById(cardId);
        board.removeCard(cardId);
        currentPlayer.getTribe().addCharacter(card); // TODO capire come gestire i tipi qui
        applyImmediateEffect(card);
        notifyChange("card_drawn");

        if (hasCurrentPlayerFinishedDrawing()) {
            advanceActionTurn();
        }
    }

    public boolean canDrawCard(int cardId) {
        // TODO REFACTOR: aggiungi controllo per non pescare carte evento
        if (currentPhase != GamePhase.ACTION) return false;

        if (board.findCardById(cardId) == null) return false;

        // la carta si trova in una riga da cui il giocatore può ancora pescare in questo turno?
        OfferTileAction action = getCurrentPlayerAction();

        if (board.getTopRow().containsCard(cardId)) {
            return cardsDrawnFromTopRow < action.getTopRowCards();
        }
        if (board.getBottomRow().containsCard(cardId)) {
            return cardsDrawnFromBottomRow < action.getBottomRowCards();
        }

        return false;
    }

    private boolean hasCurrentPlayerFinishedDrawing() {
        OfferTileAction action = getCurrentPlayerAction();

        // ha ancora carte da prendere dalla TopRow?
        boolean stillNeedsTop = cardsDrawnFromTopRow < action.getTopRowCards()
                && board.getTopRow().hasAvailableCards();

        // ha ancora carte da prendere dalla BottomRow?
        boolean stillNeedsBottom = cardsDrawnFromBottomRow < action.getBottomRowCards()
                && board.getBottomRow().hasAvailableCards();

        // il turno è finito quando non ha più nulla da pescare
        return !stillNeedsTop && !stillNeedsBottom;
    }

    public OfferTileAction getCurrentPlayerAction() {
        return board.getOfferTrack()
                .getOccupiedTileByPlayer(currentPlayer)
                .getAction();
    }

    private void applyImmediateEffect(Card card) {
        // TODO
    }

    private void advanceActionTurn() {
        cardsDrawnFromTopRow    = 0;
        cardsDrawnFromBottomRow = 0;

        board.getTurnOrderTile().returnTotem(currentPlayer);

        Player next = getNextPlayerInActionOrder();
        if (next != null) {
            currentPlayer = next;
            notifyChange("turn_changed");
        } else {
            setPhase(GamePhase.END_OF_ROUND);
        }
    }

    private Player getNextPlayerInActionOrder() {
        // scorre i totem ancora sull'OfferTrack da sinistra a destra
        // restituisce il proprietario del primo totem trovato
        // restituisce null se non ce ne sono più
        // TODO non capisco java funzionale, controllate se è giusta porca zozza
        return board.getOfferTrack()
                .getOccupiedTilesInOrder()
                .stream()
                .map(tile -> tile.getOccupant().getOwner())
                .filter(p -> p.getTotem().getLocation() == TotemLocation.OFFER_TRACK)
                .findFirst()
                .orElse(null);
    }

    private void setPhase(GamePhase phase) {
        this.currentPhase = phase;
        notifyChange("phase_changed");

        // le fasi automatiche si auto-avviano
        if (phase == GamePhase.END_OF_ROUND) resolveEndOfRound();
    }

    // END OF ROUND PHASE ----------------------------------------------------------------------------------------------

    private void resolveEndOfRound() {
        resolveEvents();
        board.endRound(tribeDeck, players.size());

        if (isGameOver()) {
            setPhase(GamePhase.END_OF_GAME);
        } else {
            currentRound++;
            setPhase(GamePhase.PLACEMENT);
        }
    }

    private void resolveEvents() {
        // delega alla BottomRow che conosce i propri eventi
        List<EventCard> events = board.getBottomRow().getSortedEvents();
        events.forEach(e -> e.resolve(players));
        notifyChange("events_resolved");
    }


    // CODICE PRECEDENTE -----------------------------------------------------------------------------------------------

    private void resolveEndOfGame()
    // calcola punti finali per ogni giocatore
    // buildingEffectRegistry.triggerEndOfGame(model)
    // determina vincitore
    // setGameState(ENDED)
    // notifyChange("game_over")

    private boolean isGameOver()
    // currentRound > 10 || tribeDeck.isEmpty()


}