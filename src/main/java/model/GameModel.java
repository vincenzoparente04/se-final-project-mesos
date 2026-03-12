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
import model.player.Player;
import java.util.List;

public class GameModel extends Observable {

    private Board board;
    private TribeDeck tribeDeck;
    private BuildingDeck buildingDeckEraI;
    private BuildingDeck buildingDeckEraII;
    private BuildingDeck buildingDeckEraIII;
    private final List<Player> players;
    private int playerCount;
    private Player currentPlayer;
    private int currentPlayerIndex;

    private GamePhase currentPhase;
    private GameState gameState;
    private int currentRound;
    private Era currentEra;

    // setup phase -----------------------------------------------------------------------------------------------------
    public void startGame(List<String> playerNames){
        createPlayers(playerNames);
        tribeDeck.initializeDeck(allCards, playerCount); // vedi in che classe sarà la lista completa delle carte (meglio tenerla in tribeDeck e passaresolo playerCount)
        buildingDeckEraI.initializeDeck(playerCount);
        buildingDeckEraII.initializeDeck(playerCount);
        buildingDeckEraIII.initializeDeck(playerCount);
        board.setupBoard(tribeDeck, buildingDeckEraI, playerCount);
        randomizeTurnOrder();
        distributeFood();
        distributePP();
        // then setPhase(PLACEMENT) con notifyChange("phase_changed")
    }

    // placement phase (actions called by the Controller) --------------------------------------------------------------
    // Il trigger è la View che riceve "phase_changed" e abilita l'interazione per currentPlayer.
    // L'utente tocca una casella dell'OfferTrack.
    // 1. valida la mossa
    // 2. esegue il piazzamento
    // 3. avanza il turno o cambia fase
    public void placeTotem(Player player, OfferTile offerTile){
        canPlaceTotem(player, tile);
        board.getOfferTrack().placeTotem(player.getTotem(), offerTile);
        notifyChange("totem_placed");
        advancePlacementTurn();
    }

    // action phase (actions called by the Controller) -----------------------------------------------------------------


    // HELPERS - setup phase
    private List<Player> createPlayers(List<String> playerNames){
        // chiama public Player(String name, PlayerColor color)
        //      che farà new Tribe() e new Totem()
    }
    private void distributeFood(){
        player.addFood() // per ogni giocatore
    }
    private void distributePP(){
        player.addPP() // per ogni giocatore
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
    public boolean canPlaceTotem(Player player, OfferTile tile){
        // Controlla:
        //player == currentPlayer          // è il turno di questo giocatore?
        //currentPhase == PLACEMENT        // siamo nella fase giusta?
        //tile.isOccupied()                // la casella è libera?
        //player.getTotem().getLocation()  // il Totem è disponibile?
        //    == TotemLocation.TURN_ORDER_TILE
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