package shared.command.lobbyCommand;

import shared.command.ClientCommand;

/**
 * Lifecycle command server-internal. Lo crea il {@code LobbyManager} quando
 * rileva la disconnessione di un player presente in una partita attiva e lo
 * impila sulla coda della session, dove il {@code GameController} lo
 * processa via {@link LobbyCommandVisitor#visit(PlayerDisconnectedCommand)}
 * applicando le mutazioni necessarie sul model (set disconnected, rimozione
 * della view, skip del turno se era il suo).
 * <p>
 * Non viaggia mai sul wire: pur estendendo l'interfaccia
 * {@link LobbyCommand} (e quindi {@link ClientCommand} Serializable), viene
 * creato e consumato dentro la stessa JVM.
 */
public record PlayerDisconnectedCommand(String playerName) implements LobbyCommand {

    @Override
    public void accept(LobbyCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
