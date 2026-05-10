package integration;

import model.GameModel;
import model.board.OfferTile;
import model.board.OfferTileAction.OfferTileAction;
import model.cards.Card;
import model.cards.characterCards.CharacterCard;
import model.enums.Era;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.player.Player;
import model.rowsManager.RowsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shared.command.ChooseColorCommand;
import shared.command.DrawCardCommand;
import shared.command.EndTurnCommand;
import shared.command.PlaceTotemCommand;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Game Flow Integration Tests")
class GameFlowTest {

    private GameModel model;

    @BeforeEach
    void setUp() throws Exception {
        model = new GameModel();
        model.startGame(List.of("Player1", "Player2"));
        completeColorChoosing();
    }

    // functions to make tests easier

    private void completeColorChoosing() throws Exception {
        TotemColor[] colors = TotemColor.values();
        int i = 0;
        while (model.getCurrentPhase() == GamePhase.COLOR_CHOOSING_PHASE) {
            Player current = model.getCurrentPlayer();
            model.handleCommand(new ChooseColorCommand(current.getName(), colors[i++].name()));
        }
    }

    /**
     * Places each player in turn-order on the first available tile.
     * Player at turn-order position 0 → tile B (leftmost for 2-player game).
     * Player at turn-order position 1 → tile C (next leftmost).
     */
    private void completePlacement() throws Exception {
        while (model.getCurrentPhase() == GamePhase.PLACEMENT) {
            Player current = model.getCurrentPlayer();
            OfferTile free = model.getBoard().getOfferTiles().stream()
                    .filter(t -> !t.isOccupied())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No free tile for placement"));
            model.handleCommand(new PlaceTotemCommand(current.getName(), free.getLetter()));
        }
    }

    /**
     * Drives the ACTION phase to completion by drawing any forced CharacterCard,
     * or calling endTurn() when no forced draw exists.
     * Also skips the PRE_END_OF_ROUND extra draw (if any player has one) by calling endTurn().
     * Returns when the game is in PLACEMENT or END_OF_GAME phase.
     */
    private void completeActionPhase() throws Exception {
        while (model.getCurrentPhase() == GamePhase.ACTION) {
            Player current = model.getCurrentPlayer();
            if (current == null) break;

            RowsManager rm = model.getRowsManager();
            OfferTile tile = model.getBoard().getOfferTrack().getOccupiedTileByPlayer(current);
            OfferTileAction action = tile.getAction();

            Optional<Card> forcedCard = rm.getAllCardsOnBoard().stream()
                    .filter(c -> c instanceof CharacterCard && action.canDraw(c, rm))
                    .findFirst();

            if (forcedCard.isPresent()) {
                model.handleCommand(new DrawCardCommand(current.getName(), forcedCard.get().getId()));
            } else {
                try {
                    model.handleCommand(new EndTurnCommand(current.getName()));
                } catch (IllegalStateException ignored) {
                    // action auto-advanced; loop will re-evaluate phase
                }
            }
        }
        // Waive extra draw if a player earned one during this round
        if (model.getCurrentPhase() == GamePhase.PRE_END_OF_ROUND) {
            Player current = model.getCurrentPlayer();
            if (current != null) {
                model.handleCommand(new EndTurnCommand(current.getName()));
            }
        }
    }

    private void completeOneRound() throws Exception {
        completePlacement();
        completeActionPhase();
    }


    @Test
    @DisplayName("One complete round returns to PLACEMENT and increments round counter to 2")
    void phaseTransitionsAfterOneFullRound() throws Exception {
        assertEquals(GamePhase.PLACEMENT, model.getCurrentPhase());
        assertEquals(1, model.getCurrentRound());

        completeOneRound();

        assertEquals(GamePhase.PLACEMENT, model.getCurrentPhase());
        assertEquals(2, model.getCurrentRound());
    }

    @Test
    @DisplayName("Characters drawn during ActionPhase appear in the players' tribes")
    void drawnCharacterCardsAppearInTribes() throws Exception {
        completePlacement();

        List<Player> players = model.getPlayers();
        int charactersBefore = players.stream()
                .mapToInt(p -> p.getTribe().getTotalCharacterCount())
                .sum();

        completeActionPhase();

        int charactersAfter = players.stream()
                .mapToInt(p -> p.getTribe().getTotalCharacterCount())
                .sum();
        assertTrue(charactersAfter > charactersBefore,
                "At least one CharacterCard should have been added to a tribe during the action phase");
    }

    @Test
    @DisplayName("EndOfRound resolves events without throwing and transitions phase back to PLACEMENT")
    void eventResolutionCompletesAndAdvancesPhase() throws Exception {
        completeOneRound();

        assertEquals(GamePhase.PLACEMENT, model.getCurrentPhase(),
                "After EndOfRound, phase should return to PLACEMENT");
    }


    @Test
    @DisplayName("Player on leftmost action tile returns first and is first in next round's turn order")
    void firstPlayerToReturnTotemLeadsNextRoundTurnOrder() throws Exception {
        // In completePlacement(), turnOrder[0] → tile B (leftmost), turnOrder[1] → tile C.
        // Tile B is processed first in action phase; that player returns first → slot 0 → first next round.
        Player expectedFirst = model.getTurnOrder().get(0);

        completeOneRound();

        assertEquals(expectedFirst, model.getTurnOrder().get(0),
                "Player who returned totem first should occupy turn-order slot 0 next round");
    }


    @Test
    @DisplayName("Game transitions to END_OF_GAME after 10 rounds and sets at least one winner")
    void gameEndsAfterTenRoundsWithWinnersDeclared() throws Exception {
        while (model.getCurrentPhase() != GamePhase.END_OF_GAME) {
            if (model.getCurrentPhase() == GamePhase.PLACEMENT) {
                completePlacement();
            } else if (model.getCurrentPhase() == GamePhase.ACTION) {
                completeActionPhase();
            } else {
                fail("Unexpected phase: " + model.getCurrentPhase());
            }
        }

        assertEquals(GamePhase.END_OF_GAME, model.getCurrentPhase());
        assertFalse(model.getWinners().isEmpty(), "At least one winner should be declared");
        assertTrue(model.getCurrentRound() > 10,
                "Round counter should exceed 10 when game ends (isGameOver checks currentRound > 10)");
    }


    @Test
    @DisplayName("Era advances from ERA_I to ERA_II within the first 4 rounds as the deck is exhausted")
    void eraAdvancesFromEraIToEraIIWithinFourRounds() throws Exception {
        // For a 2-player game, ERA_I has 21 cards. Setup draws 9 leaving 12.
        // Each endRound draws 6 more. After 3 rounds, ERA_I is exhausted and endRound draws ERA_II cards.
        assertEquals(Era.ERA_I, model.getCurrentEra());

        for (int round = 0; round < 4 && model.getCurrentPhase() != GamePhase.END_OF_GAME; round++) {
            completePlacement();
            completeActionPhase();
        }

        assertNotEquals(Era.ERA_I, model.getCurrentEra(),
                "Era should have advanced from ERA_I after the ERA_I deck is exhausted");
    }
}
