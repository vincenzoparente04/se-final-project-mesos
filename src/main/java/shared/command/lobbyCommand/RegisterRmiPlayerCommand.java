package shared.command.lobbyCommand;

import network.server.core.PlayerEntry;

import java.util.concurrent.CompletableFuture;

/**
 * Comando server-interno di registrazione di un player RMI. Lo crea
 * {@code GameServerRemoteImpl.join(...)} e lo impila sulla coda del
 * {@code LobbyManager}, che lo processa via
 * {@link LobbyCommandVisitor#visit(RegisterRmiPlayerCommand)}: come per il
 * socket, il check-e-registra del nome avviene atomicamente sul lobby-thread.
 * <p>
 * A differenza del socket, l'{@link PlayerEntry} (e la relativa
 * {@code RmiVirtualView}) è già costruito al momento del comando: non c'è I/O
 * bloccante da differire e la view RMI non alloca risorse di rete fino
 * all'{@code activateLiveness()}, quindi un eventuale REJECT non lascia leak
 * significativi.
 * <p>
 * {@code future} è il canale di risposta "ask": completato con {@code true}
 * (ACCEPT) o {@code false} (REJECT); il thread di dispatch RMI vi blocca sopra
 * con timeout.
 * <p>
 * <strong>Non viaggia mai sul wire</strong>: {@code entry} e {@code future} non
 * sono {@link java.io.Serializable}, ma il record nasce e muore nella stessa
 * JVM.
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
