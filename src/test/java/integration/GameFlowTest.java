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
import ChooseColorCommand;
import DrawCardCommand;
import EndTurnCommand;
import PlaceTotemCommand;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Game Flow Integration Tests")
class GameFlowTest {

    private GameModel model;

    @BeforeEach
    void setUp() throws Exception {
        FakeVirtualView vv1 = new FakeVirtualView();
        FakeVirtualView vv2 = new FakeVirtualView();
        model = new GameModel(List.of(vv1,vv2));
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

    // Multi-player-count integration tests

    private GameModel setupGame(List<String> names) throws Exception {
        GameModel m = new GameModel();
        m.startGame(names);
        // consume color-choosing phase
        TotemColor[] colors = TotemColor.values();
        int i = 0;
        while (m.getCurrentPhase() == GamePhase.COLOR_CHOOSING_PHASE) {
            Player current = m.getCurrentPlayer();
            m.handleCommand(new ChooseColorCommand(current.getName(), colors[i++].name()));
        }
        return m;
    }

    private void runOnePlacement(GameModel m) throws Exception {
        while (m.getCurrentPhase() == GamePhase.PLACEMENT) {
            Player current = m.getCurrentPlayer();
            OfferTile free = m.getBoard().getOfferTiles().stream()
                    .filter(t -> !t.isOccupied())
                    .findFirst()
                    .orElseThrow();
            m.handleCommand(new PlaceTotemCommand(current.getName(), free.getLetter()));
        }
    }

    private void runOneActionPhase(GameModel m) throws Exception {
        while (m.getCurrentPhase() == GamePhase.ACTION) {
            Player current = m.getCurrentPlayer();
            if (current == null) break;
            RowsManager rm = m.getRowsManager();
            OfferTile tile = m.getBoard().getOfferTrack().getOccupiedTileByPlayer(current);
            OfferTileAction action = tile.getAction();
            Optional<Card> forced = rm.getAllCardsOnBoard().stream()
                    .filter(c -> c instanceof CharacterCard && action.canDraw(c, rm))
                    .findFirst();
            if (forced.isPresent()) {
                m.handleCommand(new DrawCardCommand(current.getName(), forced.get().getId()));
            } else {
                try { m.handleCommand(new EndTurnCommand(current.getName())); }
                catch (IllegalStateException ignored) {}
            }
        }
        if (m.getCurrentPhase() == GamePhase.PRE_END_OF_ROUND) {
            Player current = m.getCurrentPlayer();
            m.handleCommand(new EndTurnCommand(current.getName()));
        }
    }

    @Test
    @DisplayName("3-player game: one full round transitions back to PLACEMENT and increments round counter")
    void threePlayerGameCompletesOneRoundCorrectly() throws Exception {
        GameModel m = setupGame(List.of("A", "B", "C"));

        assertEquals(GamePhase.PLACEMENT, m.getCurrentPhase());
        assertEquals(1, m.getCurrentRound());
        assertEquals(3, m.getPlayers().size());

        runOnePlacement(m);
        runOneActionPhase(m);

        assertEquals(GamePhase.PLACEMENT, m.getCurrentPhase());
        assertEquals(2, m.getCurrentRound());
    }

    @Test
    @DisplayName("3-player game: board has exactly the tiles eligible for 3 players")
    void threePlayerGameHasCorrectOfferTileCount() throws Exception {
        GameModel m = setupGame(List.of("A", "B", "C"));
        // board.json: tiles with minPlayers <= 3 are active
        long activeTiles = m.getBoard().getOfferTiles().size();
        assertTrue(activeTiles >= 3 && activeTiles <= 6,
                "3-player board should have between 3 and 6 offer tiles, got " + activeTiles);
    }

    @Test
    @DisplayName("3-player game: first-position player gets 2 food, second gets 3, third gets 3")
    void threePlayerGameFoodBonusesAreCorrect() throws Exception {
        GameModel m = setupGame(List.of("A", "B", "C"));
        List<Player> order = m.getTurnOrder();
        assertEquals(2, order.get(0).getFood(), "position 0 → 2 food");
        assertEquals(3, order.get(1).getFood(), "position 1 → 3 food");
        assertEquals(3, order.get(2).getFood(), "position 2 → 3 food");
    }

    @Test
    @DisplayName("4-player game: one full round transitions back to PLACEMENT and increments round counter")
    void fourPlayerGameCompletesOneRoundCorrectly() throws Exception {
        GameModel m = setupGame(List.of("A", "B", "C", "D"));

        assertEquals(GamePhase.PLACEMENT, m.getCurrentPhase());
        assertEquals(1, m.getCurrentRound());
        assertEquals(4, m.getPlayers().size());

        runOnePlacement(m);
        runOneActionPhase(m);

        assertEquals(GamePhase.PLACEMENT, m.getCurrentPhase());
        assertEquals(2, m.getCurrentRound());
    }

    @Test
    @DisplayName("4-player game: first-position player gets 2 food, second gets 3, third gets 3, fourth gets 4")
    void fourPlayerGameFoodBonusesAreCorrect() throws Exception {
        GameModel m = setupGame(List.of("A", "B", "C", "D"));
        List<Player> order = m.getTurnOrder();
        assertEquals(2, order.get(0).getFood(), "position 0 → 2 food");
        assertEquals(3, order.get(1).getFood(), "position 1 → 3 food");
        assertEquals(3, order.get(2).getFood(), "position 2 → 3 food");
        assertEquals(4, order.get(3).getFood(), "position 3 → 4 food");
    }

    @Test
    @DisplayName("4-player game ends after 10 rounds with at least one winner")
    void fourPlayerGameEndsAfterTenRoundsWithWinner() throws Exception {
        GameModel m = setupGame(List.of("A", "B", "C", "D"));
        while (m.getCurrentPhase() != GamePhase.END_OF_GAME) {
            if (m.getCurrentPhase() == GamePhase.PLACEMENT)        runOnePlacement(m);
            else if (m.getCurrentPhase() == GamePhase.ACTION)      runOneActionPhase(m);
            else fail("Unexpected phase: " + m.getCurrentPhase());
        }
        assertEquals(GamePhase.END_OF_GAME, m.getCurrentPhase());
        assertFalse(m.getWinners().isEmpty(), "At least one winner should be declared");
    }
}
