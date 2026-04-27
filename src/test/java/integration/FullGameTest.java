package integration;

import controller.GameController;
import model.GameModel;
import model.board.OfferTile;
import model.enums.Era;
import model.enums.GamePhase;
import model.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static model.enums.TotemColor.GREEN;
import static model.enums.TotemColor.RED;
import static org.junit.jupiter.api.Assertions.*;

public class FullGameTest {

    @Test
    @DisplayName("Simulate a full game with 2 players, covering all phases and verifying correct flow and state transitions")
    void fullGameTest() {
        GameModel model = new GameModel();
        GameController gameController = new GameController(model);

        List<String> playerNames = List.of("Homer", "Bart");
        gameController.startGame(playerNames);

        Player homer = model.getPlayerByName("Homer");
        Player bart = model.getPlayerByName("Bart");

        // --- Color Choosing Phase ---

        assertEquals(model.getCurrentPhase(), GamePhase.COLOR_CHOOSING_PHASE, "Phase should be color choosing phase");

        assertEquals(model.getCurrentPlayer(), homer, "Current player should be homer");
        //wrong turn case
        IllegalStateException errorTurn = assertThrows(
                IllegalStateException.class,
                () -> gameController.chooseColor("Bart", "RED")
        );
        assertTrue(errorTurn.getMessage().contains("It is not Bart's turn."));

        gameController.chooseColor("Homer", "RED");

        assertEquals(model.getCurrentPlayer(), bart, "Current player should be bart");

        //color already taken case
        IllegalArgumentException errorColorTaken = assertThrows(
                IllegalArgumentException.class,
                () -> gameController.chooseColor("Bart", "RED")
        );
        assertTrue(errorColorTaken.getMessage().contains("color RED is already taken"));

        //instruction for wrong phase
        IllegalStateException errorWrongPhaseCOLOR_CHOOSING = assertThrows(
                IllegalStateException.class,
                () -> gameController.placeTotem("Bart", 'A')
        );
        assert(errorWrongPhaseCOLOR_CHOOSING.getMessage().contains("Cannot place totem during COLOR_CHOOSING_PHASE"));


        gameController.chooseColor("Bart", "GREEN");

        assertEquals(homer.getColor(),RED, "homer's color should be RED");
        assertEquals(bart.getColor(),GREEN, "bart's color should be GREEN");



        // -- Placement Phase --

        //check if player have the correct food amount at the beginning of the game
        List<Player> turnOrder = model.getTurnOrder();

        Player p0 = turnOrder.get(0);
        Player p1 = turnOrder.get(1);

        assertEquals(p0.getFood(), 2, "player 0 should start with 2 food");
        assertEquals(p1.getFood(), 3, "Player 1 should start with 3 food");

        assertEquals(model.getCurrentPhase(), GamePhase.PLACEMENT, "Phase should be placement phase after action");
        assertEquals(model.getCurrentEra(), Era.ERA_I, "Era should be I after color choosing phase");

        assertEquals(model.getCurrentRound(), 1, "Round should be 1 at the beginning");

        //instruction for wrong phase
        IllegalStateException errorWrongPhasePLACEMENT = assertThrows(
                IllegalStateException.class,
                () -> gameController.chooseColor(p0.getName(), "RED")
        );
        assertTrue(errorWrongPhasePLACEMENT.getMessage().contains("Cannot choose color during PLACEMENT"));


        //wrong order
        IllegalStateException errorTurnOrderPlacement = assertThrows(
                IllegalStateException.class,
                () -> gameController.placeTotem(p1.getName(), 'B')
        );
        assertTrue(errorTurnOrderPlacement.getMessage().contains("It is not " + p1.getName() + "'s turn."));

        //invalid tile id
        IllegalArgumentException errorInvalidTileId = assertThrows(
                IllegalArgumentException.class,
                () -> gameController.placeTotem(p0.getName(), 'Z')
        );
        assertTrue(errorInvalidTileId.getMessage().contains("tileId Z is invalid"));

        gameController.placeTotem(p0.getName(), 'B');

        //yet occupied tile
        IllegalArgumentException errorOccupiedTile = assertThrows(
                IllegalArgumentException.class,
                () -> gameController.placeTotem(p1.getName(), 'B')
        );
        assertTrue(errorOccupiedTile.getMessage().contains("Tile B is already occupied"));

        gameController.placeTotem(p1.getName(), 'C');


        //players in correctTiles
        assertEquals(model.getBoard().findTileByLetter('B').getOccupant(), p0, "Player 0 should be on tile B");
        assertEquals(model.getBoard().findTileByLetter('C').getOccupant(), p1, "Player 1 should be on tile C");


        // --- Action Phase ---
        assertEquals(model.getCurrentPhase(), GamePhase.ACTION, "Phase should be action phase after placement");

        //wrong instruction for action phase
        IllegalStateException errorWrongPhaseACTION = assertThrows(
                IllegalStateException.class,
                () -> gameController.chooseColor(p0.getName(), "RED")
        );
        assertTrue(errorWrongPhaseACTION.getMessage().contains("Cannot choose color during ACTION"));

        // ACTION PHASE LOGIC:
        assertEquals(p0, model.getCurrentPlayer(), "Current player should be p0 (tile B)");

        int p0InvalidCardId = model.getRowsManager().getTopRowTribe().getFirst().getId();
        int p0ValidCardId = model.getRowsManager().getBottomRowTribe().getFirst().getId();

        // p0 tries to draw from the top row (invalid for Tile B)
        IllegalStateException errorTopRowDraw = assertThrows(
                IllegalStateException.class,
                () -> gameController.drawCard(p0.getName(), p0InvalidCardId)
        );
        assertTrue(errorTopRowDraw.getMessage().contains("does not allow drawing this card"));

        // p0 attempts to completely bypass drawing and end turn prematurely
        IllegalStateException errorEarlyEnd = assertThrows(
                IllegalStateException.class,
                () -> gameController.endTurn(p0.getName())
        );
        assertTrue(errorEarlyEnd.getMessage().contains("All mandatory draws must be completed"));

        // p0 performs valid draw
        int p0ValidTribeCardId = model.getRowsManager().getBottomRowTribe().stream().filter(c -> !(c instanceof model.cards.eventCards.EventCard)).findFirst().get().getId();
        assertDoesNotThrow(() -> gameController.drawCard(p0.getName(), p0ValidTribeCardId));

        // After p0's draw, it auto-advances to p1 since Tile B only allowed 1 draw
        assertEquals(p1, model.getCurrentPlayer(), "Current player should be p1 (tile C)");

        int p1InvalidCardId = model.getRowsManager().getBottomRowTribe().getFirst().getId(); // bottom row id
        int p1ValidTribeCardId = model.getRowsManager().getTopRowTribe().stream().filter(c -> !(c instanceof model.cards.eventCards.EventCard)).findFirst().get().getId();

        // p1 tries to draw from bottom row (invalid for Tile C)
        IllegalStateException errorBottomRowDraw = assertThrows(
                IllegalStateException.class,
                () -> gameController.drawCard(p1.getName(), p1InvalidCardId)
        );
        assertTrue(errorBottomRowDraw.getMessage().contains("does not allow drawing this card"));

        // p1 performs valid draw
        assertDoesNotThrow(() -> gameController.drawCard(p1.getName(), p1ValidTribeCardId));

        // After all players finish their action phase, the game should transition to PRE_END_OF_ROUND --> it goes directly to END_OF_ROUND because there isn't any player with extra draw
        //assertEquals(GamePhase.PRE_END_OF_ROUND, model.getCurrentPhase(), "Phase should transition to PRE_END_OF_ROUND");


        assertEquals(model.getCurrentRound(), 2, "Round should be 2 after first round completion");
        assertEquals(model.getCurrentPhase(), GamePhase.PLACEMENT, "Phase should be placement phase at start of round 2");

        // Loop to advance the game to the END_OF_GAME phase
        while (model.getCurrentPhase() != GamePhase.END_OF_GAME) {
            playRoundAutonomously(model, gameController);
        }

        assertEquals(GamePhase.END_OF_GAME, model.getCurrentPhase(), "Game is over and we should be in EndOfGamePhase");

        List<String> winners = model.getWinners();
        assertNotNull(winners, "Winners list should not be null at end of game");

        System.out.println("---------------------------------");
        System.out.println("winner(s): " + winners);
        System.out.println("---------------------------------");

        winners.forEach(w -> assertTrue(playerNames.contains(w), "Winner should be one of the original players"));
        int winnerPP = playerNames.stream().filter(p -> winners.contains(p)).map(p -> model.getPlayerByName(p)).map(p->p.getPrestigePoints()).findFirst().orElse(1);
        int loserPP = 0;
        for(int i = 0; i < playerNames.size(); i++){
            if(!winners.contains(playerNames.get(i))){
                loserPP = model.getPlayerByName(playerNames.get(i)).getPrestigePoints();
            }
        }
        assertEquals(winnerPP>=loserPP, true, "Winner should have more PP than loser");

        assertNotNull(model.getPhaseHandler());

    }

    private void playRoundAutonomously(GameModel model, GameController gameController) {
        // PLACEMENT PHASE
        if (model.getCurrentPhase() == GamePhase.PLACEMENT) {
            for (int i = 0; i < model.getTurnOrder().size(); i++) {
                Player p = model.getCurrentPlayer(); // The player whose turn it is to place
                if (p == null) break;
                for (OfferTile tile : model.getBoard().getOfferTrack().getTiles()) {
                    if (!tile.isOccupied()) {
                        assertDoesNotThrow(() -> gameController.placeTotem(p.getName(), tile.getLetter()));
                        break;
                    }
                }
            }
        }
        
        // PRE_END_OF_ROUND PHASE
        if (model.getCurrentPhase() == GamePhase.PRE_END_OF_ROUND) {
            Player current = model.getCurrentPlayer();
            if (current != null) {
                gameController.endTurn(current.getName());
            }
        }
        
        // ACTION PHASE
        if (model.getCurrentPhase() == GamePhase.ACTION) {
            while (model.getCurrentPhase() == GamePhase.ACTION) {
                Player current = model.getCurrentPlayer();
                if (current == null) {
                    break;
                }
                
                boolean couldDraw = false;
                try {
                    // Try to draw from top row
                    int topCardId = model.getRowsManager().getTopRowTribe().stream().filter(c -> !(c instanceof model.cards.eventCards.EventCard)).findFirst().get().getId();
                    gameController.drawCard(current.getName(), topCardId);
                    couldDraw = true;
                } catch (Exception e) {
                    // Top row not allowed or failed
                }
                
                if (!couldDraw) {
                    try {
                        // Try to draw from bottom row
                        int bottomCardId = model.getRowsManager().getBottomRowTribe().stream().filter(c -> !(c instanceof model.cards.eventCards.EventCard)).findFirst().get().getId();
                        gameController.drawCard(current.getName(), bottomCardId);
                        couldDraw = true;
                    } catch (Exception e) {
                        // Bottom row not allowed or failed
                    }
                }
                
                if (!couldDraw) {
                    // For tiles that just give food, or if finished drawing
                    try {
                        gameController.endTurn(current.getName());
                    } catch (Exception e) {}
                }
            }
        }
    }

}
