package shared.command;

/**
 * Heartbeat applicativo inviato periodicamente dal client al server
 * per segnalare che la connessione è ancora viva.
 * <p>
 * Il server tiene traccia dell'istante dell'ultimo heartbeat ricevuto
 * per ciascun player; un task periodico marca come disconnesso ogni
 * player il cui ultimo heartbeat sia più vecchio di una soglia
 * configurabile.
 *
 * @param playerName nome del player mittente
 */
public record HeartbeatCommand(String playerName) implements ClientCommand {

    @Override
    public void accept(CommandDispatcher dispatcher) throws Exception {
        dispatcher.onHeartbeatCommand(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}

