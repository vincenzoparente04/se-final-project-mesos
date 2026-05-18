package shared.command;

/**
 * Lifecycle command server-internal. Lo crea il {@code GameController} stesso
 * — schedulato dal suspension-scheduler — quando il timer di sospensione
 * scade. Impilarlo sulla coda invece di eseguire la logica nel task
 * schedulato garantisce che la chiusura della partita (setWinners,
 * setGameOver, notifyChange) avvenga sul game thread, mantenendo
 * l'invariante "solo il game thread modifica il model".
 * <p>
 * Non viaggia mai sul wire: pur estendendo l'interfaccia {@link LobbyCommand}
 * (e quindi {@link ClientCommand} Serializable), viene creato e consumato
 * dentro la stessa JVM.
 */
public record SuspensionTimeoutCommand() implements LobbyCommand {

    @Override
    public void accept(LobbyCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return null; // synthetic command, not associated to a single player
    }
}
