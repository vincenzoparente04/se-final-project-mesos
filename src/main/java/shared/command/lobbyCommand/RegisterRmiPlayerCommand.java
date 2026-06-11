package shared.command.lobbyCommand;

import network.server.core.PlayerEntry;

import java.util.concurrent.CompletableFuture;

/**
 * Server-internal registration command for an RMI player. It is created by
 * {@code GameServerRemoteImpl.join(...)} and enqueued on the
 * {@code LobbyManager} queue, which processes it via
 * {@link LobbyCommandVisitor#visit(RegisterRmiPlayerCommand)}: as with the
 * socket case, the check-and-register of the name happens atomically on the
 * lobby thread.
 * <p>
 * Unlike the socket case, the {@link PlayerEntry} (and its
 * {@code RmiVirtualView}) is already built when the command is issued: there is
 * no blocking I/O to defer and the RMI view allocates no network resources until
 * {@code activateLiveness()}, so a possible REJECT leaves no significant leak.
 * <p>
 * {@code future} is the "ask" reply channel: completed with {@code true}
 * (ACCEPT) or {@code false} (REJECT); the RMI dispatch thread blocks on it with
 * a timeout.
 * <p>
 * <strong>It never travels over the wire</strong>: {@code entry} and
 * {@code future} are not {@link java.io.Serializable}, but the record is born
 * and dies within the same JVM.
 *
 * @param playerName name requested by the RMI player
 * @param entry      already-built {@link PlayerEntry} (RMI view included)
 * @param future     "ask" channel: completed with {@code true} (ACCEPT) or
 *                   {@code false} (REJECT, name taken)
 */
public record RegisterRmiPlayerCommand(String playerName,
                                       PlayerEntry entry,
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
