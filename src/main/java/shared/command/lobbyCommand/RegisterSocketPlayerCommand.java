package shared.command.lobbyCommand;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

/**
 * Server-internal registration command for a socket player. It is created by the
 * {@code ConnectionHandshaker} (on the per-connection thread) after reading the
 * {@code ConnectMessage}, and enqueued on the {@code LobbyManager} queue, which
 * processes it via {@link LobbyCommandVisitor#visit(RegisterSocketPlayerCommand)}:
 * on the lobby thread the check-and-register of the name is a single atomic
 * action (the queue's serialization IS the mutual exclusion), removing the
 * TOCTOU race on duplicate names.
 * <p>
 * The command carries {@code socket}/{@code in}/{@code out} so that,
 * <strong>only if</strong> the name is free, the lobby thread builds
 * {@code SocketVirtualView}/{@code SocketClientHandler}/{@code SocketPlayerEntry}
 * and starts the reader thread. Building inside the "name free" branch avoids
 * allocating the player's sender executor for a rejected name and keeps the
 * socket open for the retry. None of these operations is blocking
 * ({@code Thread.start()} returns immediately), so the lobby thread never does I/O.
 * <p>
 * {@code future} is the "ask" reply channel: the lobby thread completes it with
 * {@code true} (ACCEPT, reader already started) or {@code false} (REJECT, name
 * taken); the connection thread blocks on it with a timeout.
 * <p>
 * <strong>It never travels over the wire</strong>: {@code socket}/{@code in}/{@code out}
 * and {@code future} are not {@link java.io.Serializable}, but the record is born
 * and dies within the same JVM (same contract as {@code PlayerReconnectedCommand}).
 *
 * @param playerName name requested by the socket player
 * @param socket     the connection socket, used to build the view only if the
 *                   name is free
 * @param in         the connection's input stream
 * @param out        the connection's output stream
 * @param future     "ask" channel: completed with {@code true} (ACCEPT, reader
 *                   started) or {@code false} (REJECT, name taken)
 */
public record RegisterSocketPlayerCommand(String playerName, Socket socket, ObjectInputStream in, ObjectOutputStream out,
                                          CompletableFuture<Boolean> future) implements LobbyCommand {

    @Override
    public void accept(LobbyCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
