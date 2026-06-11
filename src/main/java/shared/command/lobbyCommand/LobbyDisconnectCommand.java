package shared.command.lobbyCommand;

/**
 * Server-internal disconnection command. It is enqueued by the liveness
 * sentinels and the transport handlers (socket {@code finally} / RMI
 * {@code handleDisconnect}) through {@code LobbyManager.onDisconnect(name)},
 * which now merely enqueues. It is processed on the lobby thread via
 * {@link LobbyCommandVisitor#visit(LobbyDisconnectCommand)}, where the
 * disconnect pipeline (removal from the lobby, game suspension, view close)
 * runs serialized with everything else — removing the "enqueue-before-close"
 * ordering hack that was needed when the body ran on the sentinel thread.
 *
 * @param playerName name of the player to disconnect
 */
public record LobbyDisconnectCommand(String playerName) implements LobbyCommand {

    @Override
    public void accept(LobbyCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
