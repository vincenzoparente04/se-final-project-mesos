package controller;

import integration.FakeVirtualView;
import model.GameModel;
import model.phaseHandlers.EndOfGamePhase;
import model.phaseHandlers.GamePhaseHandler;
import model.player.Player;
import network.server.core.PlayerEntry;
import network.server.core.VirtualView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shared.command.gameCommand.ChooseColorCommand;
import shared.command.gameCommand.DrawCardCommand;
import shared.command.gameCommand.EndTurnCommand;
import shared.command.gameCommand.GameCommand;
import shared.command.gameCommand.PlaceTotemCommand;
import shared.command.lobbyCommand.HeartbeatCommand;
import shared.command.lobbyCommand.LeaveCommand;
import shared.command.lobbyCommand.PlayerDisconnectedCommand;
import shared.command.lobbyCommand.PlayerReconnectedCommand;
import shared.command.lobbyCommand.SuspensionTimeoutCommand;

import model.enums.GamePhase;
import shared.command.lobbyCommand.LobbyCommand;
import shared.command.lobbyCommand.LobbyCommandVisitor;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GameController Tests")
class GameControllerTest {

    private GameModel model;
    private GameController controller;

    private Player alice;
    private VirtualView aliceView;

    private final PrintStream originalErr = System.err;

    @BeforeEach
    void suppressStderr() { System.setErr(new PrintStream(OutputStream.nullOutputStream())); }

    @org.junit.jupiter.api.AfterEach
    void restoreStderr() { System.setErr(originalErr); }

    @BeforeEach
    void setUp() {
        model = mock(GameModel.class);
        controller = new GameController(model);

        alice = mock(Player.class);
        when(alice.getName()).thenReturn("Alice");
        when(alice.isConnected()).thenReturn(true);

        aliceView = mock(VirtualView.class);
        when(aliceView.getPlayerName()).thenReturn("Alice");

        when(model.getCurrentPlayer()).thenReturn(alice);
        when(model.getViews()).thenReturn(List.of(aliceView));
        when(model.getPlayers()).thenReturn(List.of(alice));
    }

    // ── startGame ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("startGame delegates player names to the model")
    void startGameDelegatesToModel() {
        List<String> names = List.of("Alice", "Bob");
        controller.startGame(names);
        verify(model).startGame(names);
    }

    // ── isGameOver ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isGameOver returns true when model reports game over")
    void isGameOverDelegatesToModel() {
        when(model.isGameOver()).thenReturn(true);
        assertTrue(controller.isGameOver());
    }

    @Test
    @DisplayName("isGameOver returns false when model reports game in progress")
    void isGameOverReturnsFalse() {
        when(model.isGameOver()).thenReturn(false);
        assertFalse(controller.isGameOver());
    }

    // ── handleCommand — turn validation ───────────────────────────────────────

    @Test
    @DisplayName("handleCommand delegates to model when it is the player's turn")
    void handleCommandDelegatesToModel() throws Exception {
        GameCommand cmd = new ChooseColorCommand("Alice", "RED");
        controller.handleCommand(cmd);
        verify(model).handleCommand(cmd);
    }

    @Test
    @DisplayName("handleCommand skips turn check during COLOR_CHOOSING_PHASE, any player can send commands")
    void handleCommandSkipsTurnCheckDuringColorChoosingPhase() throws Exception {
        when(model.getCurrentPhase()).thenReturn(GamePhase.COLOR_CHOOSING_PHASE);
        when(model.getCurrentPlayer()).thenReturn(null);

        // Bob is not the current player (null), but during COLOR_CHOOSING the check is bypassed
        GameCommand cmd = new ChooseColorCommand("Bob", "BLUE");
        assertDoesNotThrow(() -> controller.handleCommand(cmd));
        verify(model).handleCommand(cmd);
    }

    @Test
    @DisplayName("handleCommand throws when current player is null")
    void handleCommandThrowsWhenCurrentPlayerNull() throws Exception {
        when(model.getCurrentPlayer()).thenReturn(null);
        GameCommand cmd = new ChooseColorCommand("Alice", "RED");
        assertThrows(IllegalStateException.class, () -> controller.handleCommand(cmd));
        verify(model, never()).handleCommand(any());
    }

    @Test
    @DisplayName("handleCommand throws when it is not the player's turn")
    void handleCommandThrowsWhenWrongTurn() throws Exception {
        Player bob = mock(Player.class);
        when(bob.getName()).thenReturn("Bob");
        when(model.getCurrentPlayer()).thenReturn(bob);

        GameCommand cmd = new ChooseColorCommand("Alice", "RED");
        assertThrows(IllegalStateException.class, () -> controller.handleCommand(cmd));
        verify(model, never()).handleCommand(any());
    }

    @Test
    @DisplayName("handleCommand works for PlaceTotemCommand")
    void handleCommandPlaceTotem() throws Exception {
        GameCommand cmd = new PlaceTotemCommand("Alice", 'A');
        controller.handleCommand(cmd);
        verify(model).handleCommand(cmd);
    }

    @Test
    @DisplayName("handleCommand works for DrawCardCommand")
    void handleCommandDrawCard() throws Exception {
        GameCommand cmd = new DrawCardCommand("Alice", 42);
        controller.handleCommand(cmd);
        verify(model).handleCommand(cmd);
    }

    @Test
    @DisplayName("handleCommand works for EndTurnCommand")
    void handleCommandEndTurn() throws Exception {
        GameCommand cmd = new EndTurnCommand("Alice");
        controller.handleCommand(cmd);
        verify(model).handleCommand(cmd);
    }

    // ── visit(HeartbeatCommand) ───────────────────────────────────────────────

    @Test
    @DisplayName("visit(HeartbeatCommand) is a no-op")
    void visitHeartbeatIsNoOp() throws Exception {
        assertDoesNotThrow(() -> controller.visit(new HeartbeatCommand("Alice")));
        verify(model, never()).handleCommand(any());
    }

    // ── visit(GameCommand) ────────────────────────────────────────────────────

    @Test
    @DisplayName("visit(GameCommand) dispatches to handleCommand when game is active")
    void visitGameCommandDelegatesToHandleCommand() throws Exception {
        GameCommand cmd = new ChooseColorCommand("Alice", "RED");
        controller.visit(cmd);
        verify(model).handleCommand(cmd);
    }

    @Test
    @DisplayName("visit(GameCommand) silently drops command when current player is disconnected")
    void visitGameCommandWhenCurrentPlayerDisconnectedIgnoresCommand() throws Exception {
        when(alice.isConnected()).thenReturn(false);
        GameCommand cmd = new ChooseColorCommand("Alice", "RED");
        controller.visit(cmd);
        verify(model, never()).handleCommand(any());
    }

    @Test
    @DisplayName("visit(GameCommand) sends error to view when handleCommand throws")
    void visitGameCommandWhenHandleCommandThrowsSendsErrorToView() throws Exception {
        doThrow(new IllegalStateException("bad move")).when(model).handleCommand(any());
        GameCommand cmd = new ChooseColorCommand("Alice", "RED");
        controller.visit(cmd);
        verify(aliceView).sendError("bad move");
    }

    @Test
    @DisplayName("visit(GameCommand) sends exception class name when exception has no message")
    void visitGameCommandExceptionWithNullMessageSendsClassName() throws Exception {
        doThrow(new RuntimeException()).when(model).handleCommand(any());
        GameCommand cmd = new ChooseColorCommand("Alice", "RED");
        controller.visit(cmd);
        verify(aliceView).sendError("RuntimeException");
    }

    @Test
    @DisplayName("visit(GameCommand) sends GAME_SUSPENDED error when game is suspended")
    void visitGameCommandWhenSuspendedSendsError() throws Exception {
        // Trigger suspension: disconnect Bob → only Alice remains connected
        Player bob = mock(Player.class);
        when(bob.getName()).thenReturn("Bob");
        when(model.getPlayerByName("Bob")).thenReturn(bob);
        when(model.getPhaseHandler()).thenReturn(null);
        // After disconnect countConnected() = 1 (Alice is connected)
        when(model.getPlayers()).thenReturn(List.of(alice));

        controller.visit(new PlayerDisconnectedCommand("Bob"));
        // controller.suspended is now true

        GameCommand cmd = new ChooseColorCommand("Alice", "RED");
        controller.visit(cmd);

        // The key assertion: the command was NOT forwarded to the model
        verify(model, never()).handleCommand(cmd);
        // GAME_SUSPENDED is sent at least by visit(GameCommand) when blocked
        verify(aliceView, org.mockito.Mockito.atLeastOnce()).sendError(contains("GAME_SUSPENDED"));
    }

    // ── visit(LobbyCommand) → LeaveCommand ───────────────────────────────────

    @Test
    @DisplayName("LeaveCommand when game is over disconnects player and notifies others")
    void leaveCommandWhenGameOverDisconnectsAndNotifies() throws Exception {
        VirtualView bobView = mock(VirtualView.class);
        when(bobView.getPlayerName()).thenReturn("Bob");

        when(model.isGameOver()).thenReturn(true);
        when(model.getPlayerByName("Alice")).thenReturn(alice);
        when(model.getViews()).thenReturn(List.of(aliceView, bobView));

        controller.visit(new LeaveCommand("Alice"));

        verify(alice).setDisconnected();
        verify(model).removeView("Alice");
        // The handler sends to all registered views (no filtering of the leaver)
        verify(bobView).sendError(contains("player_left:Alice"));
    }

    @Test
    @DisplayName("LeaveCommand when game is not over is a no-op")
    void leaveCommandWhenGameNotOverIsNoOp() throws Exception {
        when(model.isGameOver()).thenReturn(false);

        controller.visit(new LeaveCommand("Alice"));

        verify(model, never()).removeView(any());
        verify(alice, never()).setDisconnected();
    }

    @Test
    @DisplayName("LeaveCommand with unknown player name is safely ignored")
    void leaveCommandWithUnknownPlayerIsIgnored() throws Exception {
        when(model.isGameOver()).thenReturn(true);
        when(model.getPlayerByName("Ghost")).thenThrow(new IllegalArgumentException("No player"));

        assertDoesNotThrow(() -> controller.visit(new LeaveCommand("Ghost")));
        verify(model, never()).removeView(any());
    }

    // ── visit(LobbyCommand) → PlayerDisconnectedCommand ──────────────────────

    @Test
    @DisplayName("PlayerDisconnectedCommand marks player disconnected, removes view, notifies others")
    void playerDisconnectedSetsStateAndNotifiesOthers() throws Exception {
        Player bob = mock(Player.class);
        when(bob.getName()).thenReturn("Bob");
        VirtualView bobView = mock(VirtualView.class);
        when(bobView.getPlayerName()).thenReturn("Bob");

        when(model.getPlayerByName("Bob")).thenReturn(bob);
        when(model.getViews()).thenReturn(List.of(aliceView, bobView));
        when(model.getPhaseHandler()).thenReturn(null);
        // 0 remaining connected → triggers game-over path (else-if branch)
        when(model.getPlayers()).thenReturn(List.of());

        controller.visit(new PlayerDisconnectedCommand("Bob"));

        verify(bob).setDisconnected();
        verify(model).removeView("Bob");
        // Only Alice (not Bob) gets the disconnect notification
        verify(aliceView).sendError(contains("Player_disconnected:Bob"));
        verify(bobView, never()).sendError(any());
    }

    @Test
    @DisplayName("PlayerDisconnectedCommand with unknown player name is safely ignored")
    void playerDisconnectedWithUnknownPlayerIsIgnored() throws Exception {
        when(model.getPlayerByName("Ghost")).thenThrow(new IllegalArgumentException());

        assertDoesNotThrow(() -> controller.visit(new PlayerDisconnectedCommand("Ghost")));
        verify(model, never()).removeView(any());
    }

    @Test
    @DisplayName("PlayerDisconnectedCommand skips current player turn when it is their turn")
    void playerDisconnectedSkipsCurrentPlayerTurnWhenItIsTheirTurn() throws Exception {
        GamePhaseHandler phase = mock(GamePhaseHandler.class);
        when(phase.getCurrentPlayer()).thenReturn(alice);

        when(model.getPlayerByName("Alice")).thenReturn(alice);
        when(model.getViews()).thenReturn(List.of());
        when(model.getPhaseHandler()).thenReturn(phase);
        when(model.getPlayers()).thenReturn(List.of());

        controller.visit(new PlayerDisconnectedCommand("Alice"));

        verify(phase).skipCurrentPlayerTurn();
    }

    @Test
    @DisplayName("PlayerDisconnectedCommand does not skip turn when disconnected player is not the current one")
    void playerDisconnectedDoesNotSkipTurnWhenNotCurrentPlayer() throws Exception {
        Player bob = mock(Player.class);
        when(bob.getName()).thenReturn("Bob");

        GamePhaseHandler phase = mock(GamePhaseHandler.class);
        when(phase.getCurrentPlayer()).thenReturn(bob); // Bob is current, Alice disconnects

        when(model.getPlayerByName("Alice")).thenReturn(alice);
        when(model.getViews()).thenReturn(List.of());
        when(model.getPhaseHandler()).thenReturn(phase);
        when(model.getPlayers()).thenReturn(List.of());

        controller.visit(new PlayerDisconnectedCommand("Alice"));

        verify(phase, never()).skipCurrentPlayerTurn();
    }

    @Test
    @DisplayName("PlayerDisconnectedCommand does not skip turn when phase handler has no current player")
    void playerDisconnectedDoesNotSkipTurnWhenPhaseHasNoCurrentPlayer() throws Exception {
        GamePhaseHandler phase = mock(GamePhaseHandler.class);
        when(phase.getCurrentPlayer()).thenReturn(null);

        when(model.getPlayerByName("Alice")).thenReturn(alice);
        when(model.getViews()).thenReturn(List.of());
        when(model.getPhaseHandler()).thenReturn(phase);
        when(model.getPlayers()).thenReturn(List.of());

        controller.visit(new PlayerDisconnectedCommand("Alice"));

        verify(phase, never()).skipCurrentPlayerTurn();
    }

    @Test
    @DisplayName("PlayerDisconnectedCommand triggers suspension when exactly one player remains connected")
    void playerDisconnectedTriggersSuspensionWhenOnePlayerRemains() throws Exception {
        Player bob = mock(Player.class);
        when(bob.getName()).thenReturn("Bob");
        when(model.getPlayerByName("Bob")).thenReturn(bob);
        when(model.getViews()).thenReturn(List.of(aliceView));
        when(model.getPhaseHandler()).thenReturn(null);
        // countConnected() = 1 (Alice is still connected)
        when(model.getPlayers()).thenReturn(List.of(alice));
        when(alice.isConnected()).thenReturn(true);

        controller.visit(new PlayerDisconnectedCommand("Bob"));

        verify(aliceView).sendError(contains("GAME_SUSPENDED"));
    }

    @Test
    @DisplayName("PlayerDisconnectedCommand cancels pending suspension timer when last player also disconnects")
    void playerDisconnectedCancelsSuspensionTimerWhenLastPlayerLeaves() throws Exception {
        Player bob = mock(Player.class);
        when(bob.getName()).thenReturn("Bob");
        when(bob.isConnected()).thenReturn(false);

        // Step 1: Bob disconnects → Alice is the only one left → suspension triggered, future scheduled
        when(model.getPlayerByName("Bob")).thenReturn(bob);
        when(model.getViews()).thenReturn(List.of(aliceView));
        when(model.getPhaseHandler()).thenReturn(null);
        when(model.getPlayers()).thenReturn(List.of(alice));
        when(alice.isConnected()).thenReturn(true);
        controller.visit(new PlayerDisconnectedCommand("Bob")); // suspended = true, future scheduled

        // Step 2: Alice also disconnects → connected == 0 → cancelSuspensionTimer() on a real future
        when(model.getPlayerByName("Alice")).thenReturn(alice);
        when(model.getViews()).thenReturn(List.of());
        when(model.getPlayers()).thenReturn(List.of());
        controller.visit(new PlayerDisconnectedCommand("Alice")); // cancels the pending future

        verify(model).setGameOver();
        verify(model).setPhase(argThat(h -> h instanceof EndOfGamePhase));

        // After cancellation, a SuspensionTimeoutCommand (if it somehow fires) is a no-op
        assertDoesNotThrow(() -> controller.visit(new SuspensionTimeoutCommand()));
        verify(model).setGameOver(); // still only once
    }

    @Test
    @DisplayName("PlayerDisconnectedCommand ends game immediately when zero players remain connected")
    void playerDisconnectedEndsGameWhenNoPlayersRemain() throws Exception {
        when(model.getPlayerByName("Alice")).thenReturn(alice);
        when(model.getViews()).thenReturn(List.of());
        when(model.getPhaseHandler()).thenReturn(null);
        // countConnected() = 0
        when(model.getPlayers()).thenReturn(List.of());

        controller.visit(new PlayerDisconnectedCommand("Alice"));

        verify(model).setGameOver();
        verify(model).setPhase(argThat(h -> h instanceof EndOfGamePhase));
    }

    // ── visit(LobbyCommand) → PlayerReconnectedCommand ───────────────────────

    @Test
    @DisplayName("PlayerReconnectedCommand swaps view, marks player connected and notifies state")
    void playerReconnectedSwapsViewAndNotifies() throws Exception {
        VirtualView newView = mock(VirtualView.class);
        when(newView.getPlayerName()).thenReturn("Alice");

        when(model.getPlayerByName("Alice")).thenReturn(alice);
        when(model.getPlayers()).thenReturn(List.of(alice));
        when(model.isGameOver()).thenReturn(false);

        controller.visit(new PlayerReconnectedCommand("Alice", newView));

        verify(model).swapView("Alice", newView);
        verify(alice).setConnected();
        verify(model).notifyChange();
        verify(model, never()).notifyEndGame();
    }

    @Test
    @DisplayName("PlayerReconnectedCommand with unknown player is safely ignored")
    void playerReconnectedWithUnknownPlayerIsIgnored() throws Exception {
        VirtualView newView = mock(VirtualView.class);
        when(model.getPlayerByName("Ghost")).thenThrow(new IllegalArgumentException());

        assertDoesNotThrow(() -> controller.visit(new PlayerReconnectedCommand("Ghost", newView)));
        // swapView is called before getPlayerByName, so we verify notifyChange is never called
        verify(model, never()).notifyChange();
    }

    @Test
    @DisplayName("PlayerReconnectedCommand also sends end-game payload when game is already over")
    void playerReconnectedSendsEndGameWhenGameIsOver() throws Exception {
        VirtualView newView = mock(VirtualView.class);
        when(model.getPlayerByName("Alice")).thenReturn(alice);
        when(model.getPlayers()).thenReturn(List.of(alice));
        when(model.isGameOver()).thenReturn(true);

        controller.visit(new PlayerReconnectedCommand("Alice", newView));

        verify(model).notifyChange();
        verify(model).notifyEndGame();
    }

    @Test
    @DisplayName("PlayerReconnectedCommand resumes suspension and notifies all views when two players reconnect")
    void playerReconnectedResumesSuspension() throws Exception {
        Player bob = mock(Player.class);
        when(bob.getName()).thenReturn("Bob");
        VirtualView bobView = mock(VirtualView.class);
        when(bobView.getPlayerName()).thenReturn("Bob");

        // Step 1: disconnect Bob to trigger suspension (Alice is the 1 remaining)
        when(model.getPlayerByName("Bob")).thenReturn(bob);
        when(model.getViews()).thenReturn(List.of(aliceView));
        when(model.getPhaseHandler()).thenReturn(null);
        when(model.getPlayers()).thenReturn(List.of(alice));
        when(alice.isConnected()).thenReturn(true);
        controller.visit(new PlayerDisconnectedCommand("Bob")); // suspended = true

        // Step 2: Bob reconnects → now 2 players connected → suspension lifted
        VirtualView newBobView = mock(VirtualView.class);
        when(newBobView.getPlayerName()).thenReturn("Bob");
        when(model.getPlayerByName("Bob")).thenReturn(bob);
        when(model.getPlayers()).thenReturn(List.of(alice, bob));
        when(bob.isConnected()).thenReturn(true);
        when(model.getViews()).thenReturn(List.of(aliceView, newBobView));
        when(model.isGameOver()).thenReturn(false);

        controller.visit(new PlayerReconnectedCommand("Bob", newBobView));

        verify(aliceView).sendError(contains("GAME_RESUMED"));
        verify(newBobView).sendError(contains("GAME_RESUMED"));
    }

    // ── visit(LobbyCommand) → SuspensionTimeoutCommand ───────────────────────

    @Test
    @DisplayName("SuspensionTimeoutCommand when not suspended is a no-op")
    void suspensionTimeoutWhenNotSuspendedIsNoOp() throws Exception {
        controller.visit(new SuspensionTimeoutCommand());

        verify(model, never()).setGameOver();
        verify(model, never()).setPhase(any());
    }

    @Test
    @DisplayName("SuspensionTimeoutCommand when suspended ends game with last connected player as winner")
    void suspensionTimeoutWhenSuspendedEndsGame() throws Exception {
        // Trigger suspension first
        Player bob = mock(Player.class);
        when(bob.getName()).thenReturn("Bob");
        when(model.getPlayerByName("Bob")).thenReturn(bob);
        when(model.getViews()).thenReturn(List.of(aliceView));
        when(model.getPhaseHandler()).thenReturn(null);
        when(model.getPlayers()).thenReturn(List.of(alice));
        when(alice.isConnected()).thenReturn(true);
        controller.visit(new PlayerDisconnectedCommand("Bob")); // suspended = true

        // Fire the timeout
        controller.visit(new SuspensionTimeoutCommand());

        verify(model).setGameOver();
        verify(model).setPhase(argThat(h -> h instanceof EndOfGamePhase));
    }

    @Test
    @DisplayName("SuspensionTimeoutCommand picks no winner when all players are disconnected")
    void suspensionTimeoutWithNoConnectedPlayerEndsGameWithNullWinner() throws Exception {
        // Trigger suspension
        Player bob = mock(Player.class);
        when(bob.getName()).thenReturn("Bob");
        when(model.getPlayerByName("Bob")).thenReturn(bob);
        when(model.getViews()).thenReturn(List.of(aliceView));
        when(model.getPhaseHandler()).thenReturn(null);
        when(model.getPlayers()).thenReturn(List.of(alice));
        when(alice.isConnected()).thenReturn(true);
        controller.visit(new PlayerDisconnectedCommand("Bob")); // suspended = true

        // All players disconnect before timeout fires
        when(alice.isConnected()).thenReturn(false);
        when(model.getPlayers()).thenReturn(List.of(alice));

        controller.visit(new SuspensionTimeoutCommand());

        verify(model).setGameOver();
        // EndOfGamePhase is still set (with null winner list)
        verify(model).setPhase(argThat(h -> h instanceof EndOfGamePhase));
    }

    // ── getQueue / getGameCommandQueue ────────────────────────────────────────

    @Test
    @DisplayName("getQueue returns a non-null BlockingQueue")
    void getQueueReturnsNonNull() {
        assertNotNull(controller.getQueue());
    }

    @Test
    @DisplayName("getGameCommandQueue returns the same underlying queue as getQueue")
    void getGameCommandQueueReturnsSameInstance() {
        assertSame(controller.getQueue(), controller.getGameCommandQueue());
    }

    // ── production constructor ─────────────────────────────────────────────────

    @Test
    @DisplayName("Production constructor builds a controller with a working queue and model")
    void productionConstructorBuildsController() {
        PlayerEntry p1 = makeEntry("Player1", new FakeVirtualView());
        PlayerEntry p2 = makeEntry("Player2", new FakeVirtualView());

        GameController prod = new GameController(List.of(p1, p2));

        assertFalse(prod.isGameOver());
        assertNotNull(prod.getQueue());
    }

    @Test
    @DisplayName("Production constructor: start wires queues and shutdown cleanly stops the thread")
    void productionConstructorStartAndShutdown() throws InterruptedException {
        PlayerEntry p1 = makeEntry("Player1", new FakeVirtualView());
        PlayerEntry p2 = makeEntry("Player2", new FakeVirtualView());

        GameController prod = new GameController(List.of(p1, p2));
        prod.start();
        // Give the game thread a moment to start, then shut it down cleanly
        prod.shutdown();
        // No exception = success; thread terminated within the 2 s join window
    }

    @Test
    @DisplayName("shutdown() handles InterruptedException from gameThread.join() gracefully")
    void shutdownHandlesInterruptedJoin() {
        PlayerEntry p1 = makeEntry("Player1", new FakeVirtualView());
        PlayerEntry p2 = makeEntry("Player2", new FakeVirtualView());

        GameController prod = new GameController(List.of(p1, p2));
        prod.start();

        // Set interrupt flag on the test thread so that gameThread.join(2000) inside
        // shutdown() immediately throws InterruptedException → exercises the catch block.
        Thread.currentThread().interrupt();
        prod.shutdown();
        // Clear the interrupt flag so it doesn't bleed into other tests
        Thread.interrupted();
    }

    // ── run() game loop ───────────────────────────────────────────────────────

    @Test
    @DisplayName("run() processes commands from the queue until interrupted from outside")
    void runLoopProcessesCommandsThenExitsOnInterrupt() throws InterruptedException {
        Thread gameThread = new Thread(controller, "test-game-thread");
        gameThread.setDaemon(true);
        gameThread.start();

        // Wait until the thread is truly blocked in queue.take() before interrupting
        waitUntilBlocked(gameThread);
        gameThread.interrupt();
        gameThread.join(2000);

        assertFalse(gameThread.isAlive(), "Game thread should have terminated after interrupt");
    }

    @Test
    @DisplayName("run() catch(InterruptedException) fires when game thread is interrupted while waiting on queue")
    void runLoopCatchInterruptedExceptionFromQueueTake() throws Exception {
        // A command that sets the interrupt flag on the game thread itself.
        // After the command returns, run() loops back and queue.take() immediately
        // throws InterruptedException (flag already set) → caught by catch(InterruptedException ie).
        LobbyCommand selfInterruptCmd = new LobbyCommand() {
            @Override public String getPlayerName() { return "self-interrupt"; }
            @Override public void accept(LobbyCommandVisitor visitor) {
                Thread.currentThread().interrupt();
            }
        };

        controller.getQueue().put(selfInterruptCmd);

        Thread gameThread = new Thread(controller, "test-game-thread");
        gameThread.setDaemon(true);
        gameThread.start();
        gameThread.join(2000);

        assertFalse(gameThread.isAlive(), "Thread must exit via InterruptedException in queue.take()");
    }

    @Test
    @DisplayName("run() catch(InterruptedException) fires when interrupt propagates from inside cmd.accept()")
    void runLoopInterruptedExceptionFromCommandAccept() throws Exception {
        // A LobbyCommand that blocks forever (Thread.sleep) so that interrupting the game
        // thread causes an InterruptedException to propagate through cmd.accept(this) into
        // run()'s catch(InterruptedException) handler.
        CountDownLatch executing = new CountDownLatch(1);
        LobbyCommand blockingCmd = new LobbyCommand() {
            @Override public String getPlayerName() { return "blocker"; }
            @Override public void accept(LobbyCommandVisitor visitor) throws Exception {
                executing.countDown();
                Thread.sleep(Long.MAX_VALUE); // blocks until interrupted
            }
        };

        controller.getQueue().put(blockingCmd);

        Thread gameThread = new Thread(controller, "test-game-thread");
        gameThread.setDaemon(true);
        gameThread.start();

        // Wait until the command is being executed (sleep started), then interrupt
        assertTrue(executing.await(2, TimeUnit.SECONDS), "Command should start executing");
        gameThread.interrupt();
        gameThread.join(2000);

        assertFalse(gameThread.isAlive());
    }

    @Test
    @DisplayName("run() catches unexpected exceptions from LobbyCommand dispatch and keeps running")
    void runLoopCatchesUnexpectedExceptionAndContinues() throws Exception {
        // A LobbyCommand whose accept(LobbyCommandVisitor) throws — bypasses all internal try-catch
        // in GameController.visit(LobbyCommand) and is caught by run()'s generic handler
        LobbyCommand throwingCmd = new LobbyCommand() {
            @Override public String getPlayerName() { return "thrower"; }
            @Override public void accept(LobbyCommandVisitor visitor) throws Exception {
                throw new RuntimeException("unexpected lobby error");
            }
        };

        // Put the throwing command followed by a normal heartbeat to prove the loop continues
        controller.getQueue().put(throwingCmd);
        controller.getQueue().put(new HeartbeatCommand("Alice"));

        Thread gameThread = new Thread(controller, "test-game-thread");
        gameThread.setDaemon(true);
        gameThread.start();

        // Wait for both commands to be consumed, then interrupt cleanly
        Thread.sleep(200);
        waitUntilBlocked(gameThread);
        gameThread.interrupt();
        gameThread.join(2000);

        assertFalse(gameThread.isAlive(), "Thread should exit cleanly after interrupt");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Spin until the thread is WAITING or TIMED_WAITING (blocked in queue.take()). */
    private static void waitUntilBlocked(Thread t) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            Thread.State s = t.getState();
            if (s == Thread.State.WAITING || s == Thread.State.TIMED_WAITING) return;
            Thread.sleep(5);
        }
    }

    private static PlayerEntry makeEntry(String name, VirtualView view) {
        return new PlayerEntry() {
            @Override public String getName()    { return name; }
            @Override public VirtualView getView() { return view; }
        };
    }
}
