package server.rmi;

import server.core.VirtualView;
import shared.dto.GameStateDto;
import shared.rmi.ClientCallbackRemote;

import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * {@link VirtualView} implementation for RMI connections.
 * <p>
 * Sends state and error messages to the client by invoking the
 * {@link ClientCallbackRemote} stub asynchronously via a thread pool,
 * so a slow or disconnected RMI client never blocks the broadcast to
 * the other players.
 * <p>
 * The disconnect callback is initially a no-op and is replaced at game
 * start by {@link #setOnDisconnect(Consumer)} so that
 * {@link server.core.GameSession} can be notified when a
 * {@link RemoteException} signals a lost connection.
 * <p>
 * {@link #close()} and {@link #handleDisconnect()} are idempotent: the
 * disconnect callback fires at most once, guaranteed by the
 * {@link AtomicBoolean} {@code closed}.
 */
public class RmiVirtualView implements VirtualView {

    private final String playerName;
    private final ClientCallbackRemote callback;
    private final ExecutorService callbackExecutor;
    private final AtomicReference<Consumer<String>> onDisconnect =
            new AtomicReference<>(ignored -> {});
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public RmiVirtualView(String playerName,
                          ClientCallbackRemote callback,
                          ExecutorService callbackExecutor) {
        this.playerName = playerName;
        this.callback = callback;
        this.callbackExecutor = callbackExecutor;
    }

    /**
     * Sets the handler invoked when a {@link RemoteException} indicates
     * that this client has disconnected.  Called by
     * {@link server.core.GameSession} during start-up, before any commands
     * are processed.
     */
    public void setOnDisconnect(Consumer<String> handler) {
        onDisconnect.set(handler);
    }

    @Override
    public void sendState(GameStateDto dto) {
        if (closed.get()) return;
        callbackExecutor.submit(() -> {
            try {
                callback.onState(dto);
                if (dto.winners != null && !dto.winners.isEmpty()) {
                    callback.onGameOver(String.join(",", dto.winners));
                }
            } catch (RemoteException e) {
                handleDisconnect();
            }
        });
    }

    @Override
    public void sendError(String message) {
        if (closed.get()) return;
        callbackExecutor.submit(() -> {
            try {
                callback.onError(message);
            } catch (RemoteException e) {
                handleDisconnect();
            }
        });
    }

    @Override
    public void sendWaiting(int current, int expected) {
        if (closed.get()) return;
        callbackExecutor.submit(() -> {
            try {
                callback.onWaiting(current + ":" + expected);
            } catch (RemoteException e) {
                handleDisconnect();
            }
        });
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public void close() {
        closed.set(true);
    }

    private void handleDisconnect() {
        if (closed.compareAndSet(false, true)) {
            onDisconnect.get().accept(playerName);
        }
    }
}
