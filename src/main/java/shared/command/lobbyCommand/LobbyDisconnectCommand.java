package shared.command.lobbyCommand;

/**
 * Comando server-interno di disconnessione. Lo impilano i sentinel di liveness
 * e gli handler di trasporto (socket {@code finally} / RMI
 * {@code handleDisconnect}) tramite {@code LobbyManager.onDisconnect(name)},
 * che ora si limita a enqueue. Viene processato sul lobby-thread via
 * {@link LobbyCommandVisitor#visit(LobbyDisconnectCommand)}, dove la pipeline di
 * disconnect (rimozione dal lobby, sospensione partita, close della view) gira
 * serializzata con tutto il resto — eliminando l'hack sull'ordine
 * "enqueue-prima-di-close" che serviva quando il corpo girava sul thread del
 * sentinel.
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
