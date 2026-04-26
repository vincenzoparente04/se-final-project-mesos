package shared.rmi;

import shared.command.GameCommand;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI remote interface exposed by the server.
 * <p>
 * Clients use this stub to join the lobby and submit commands during the game.
 * Both methods return immediately: {@link #join} registers the player and
 * returns as soon as the lobby has acknowledged the entry; {@link #submitCommand}
 * enqueues the command and returns before it is processed, keeping the client
 * unblocked.
 */
public interface GameServerRemote extends Remote {

    /**
     * Registers a player in the lobby.
     * The server sends {@code WAITING} callbacks until enough players have
     * joined, then sends the first {@code STATE} callback once the game starts.
     *
     * @param playerName unique display name for this player
     * @param callback   the stub the server will use to push events back
     * @throws RemoteException on network failure
     */
    void join(String playerName, ClientCallbackRemote callback) throws RemoteException;

    /**
     * Enqueues a game command for sequential processing.
     * Returns immediately once the command is in the queue.
     *
     * @param command the command to execute
     * @throws RemoteException on network failure
     */
    void submitCommand(GameCommand command) throws RemoteException;
}
