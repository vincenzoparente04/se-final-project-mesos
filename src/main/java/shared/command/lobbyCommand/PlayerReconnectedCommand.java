package shared.command.lobbyCommand;

import network.server.core.VirtualView;

/**
 * Lifecycle command server-internal. Lo crea il {@code LobbyManager} al
 * momento di una riconnessione (handshake socket o RMI con nome già presente
 * in una partita attiva) e lo impila sulla coda della session, dove il
 * {@code GameController} lo processa via
 * {@link LobbyCommandVisitor#visit(PlayerReconnectedCommand)}: registra la
 * nuova view sul model, marca il player come riconnesso e inoltra lo stato
 * corrente.
 * <p>
 * <strong>Non viaggia mai sul wire</strong>: il componente {@code newView}
 * referenzia un {@link VirtualView} che non è {@link java.io.Serializable},
 * ma il record è costruito e consumato all'interno della stessa JVM e
 * non viene mai passato a un {@code ObjectOutputStream}. L'estensione di
 * {@link LobbyCommand} è puramente per uniformità di tipo nella coda dei
 * comandi del controller.
 */
public record PlayerReconnectedCommand(String playerName, VirtualView newView) implements LobbyCommand {

    @Override
    public void accept(LobbyCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
