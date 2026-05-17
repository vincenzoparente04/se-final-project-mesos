package shared.message;

/**
 * Messaggio heartbeat inviato periodicamente dal server al client per segnalare che la
 * connessione è ancora viva. Simmetrico a {@link shared.command.HeartbeatCommand} (client→server).
 *
 * <p>Il ricevitore lato client tratta questo messaggio come semplice prova di vita:
 * chiama {@link shared.liveness.LivenessSentinel#notifyInbound()} e non esegue
 * nessuna altra azione applicativa.
 */
public record HeartbeatMessage() implements ServerMessage {
    @Override
    public void accept(ServerMessageHandler handler) {
        handler.handle(this);
    }
}
