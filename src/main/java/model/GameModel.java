package model;

import javafx.beans.Observable;
import model.board.Board;
import model.board.OfferTile;
import model.cards.BuilderCard;
import model.cards.BuildingCard;
import model.cards.Card;
import model.deck.BuildingDeck;
import model.deck.TribeDeck;
import model.enums.Era;
import model.enums.GamePhase;
import model.enums.GameState;
import model.enums.TotemColor;
import model.player.Player;
import model.player.Totem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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

    // placement phase (actions called by the Controller) --------------------------------------------------------------
    // Il trigger è la View che riceve "phase_changed" e abilita l'interazione per currentPlayer.
    // L'utente tocca una casella dell'OfferTrack.
    // 1. valida la mossa
    // 2. esegue il piazzamento
    // 3. avanza il turno o cambia fase
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
            canPlaceTotem(player, tile);
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

    // action phase (actions called by the Controller) -----------------------------------------------------------------


    // HELPERS - setup phase
    private List<Player> createPlayers(List<String> playerNames){
        // chiama public Player(String name, PlayerColor color)
        //      che farà new Tribe() e new Totem()

        //this.players = new ArrayList<>();
        for (String name : playerNames) {
            players.add(new Player(name));
        }
        return players;
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


    // HELPERS - placement phase
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
        board.getOfferTrack().getOccupiedTilesInOrder().get(0).getOccupant().getOwner();
    }


    // CODICE PRECEDENTE -----------------------------------------------------------------------------------------------


    // action phase (called by the Controller)
    public boolean canDrawCard(Player player, Card card)
    public void drawCard(Player player, Card card)
    // delegates to board.removeCard(card)
    // delegates to player.getTribe().addCard(card)
    // applies immediate effects if present

    public boolean canDrawBuildingCard(Player player, BuildingCard card)
    public void drawBuildingCard(Player player, BuildingCard card)
    // delegates to board.removeBuildingCard(card)
    // delegates to player.getTribe().addBuildingCard(card)
    // applies immediate effects if present


    // DA RIVEDERE, PROBABILMENTE METODO INUTILE E METTIAMO UN ALTRA LOGICA PER PASSARE ALLA NUOVA FASE
    public boolean canConfirmAction(Player player)
    public void confirmAction(Player player)
    // il giocatore ha finito di prendere carte
    // TurnOrderTile.returnTotem(player)
    // se tutti hanno confermato → setPhase(BONUS_ACTION o EVENT_RESOLUTION)

    // ── fasi automatiche ─────────────────────────────────────────
    // REFACTOR, DA RIVEDERE TUTTO, VEDI APPUNTI SU WHATSAPP

    private void setPhase(GamePhase phase)
    // aggiorna currentPhase
    // notifyChange("phase_changed")
    // se fase automatica → la risolve subito:
    //   EVENT_RESOLUTION → resolveEvents()
    //   ROW_CLEANUP      → cleanupRound()
    //   END_OF_GAME      → resolveEndOfGame()

    private void resolveEvents()
    // board.getBottomRow().resolveEvents(players, buildingEffectRegistry)
    // poi setPhase(ROW_CLEANUP)

    private void cleanupRound()
    // board.endRound(tribeDeck, getPlayerCount())
    // currentRound++
    // se isGameOver() → setPhase(END_OF_GAME)
    // altrimenti → setPhase(PLACEMENT)

    private void resolveEndOfGame()
    // calcola punti finali per ogni giocatore
    // buildingEffectRegistry.triggerEndOfGame(model)
    // determina vincitore
    // setGameState(ENDED)
    // notifyChange("game_over")

    private boolean isGameOver()
    // currentRound > 10 || tribeDeck.isEmpty()


}