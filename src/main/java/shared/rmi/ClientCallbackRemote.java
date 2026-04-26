package shared.rmi;

import shared.dto.GameStateDto;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI remote interface implemented by each client.
 * <p>
 * The server holds a stub of this interface for every RMI player and calls
 * its methods asynchronously via a thread pool so that a slow or disconnected
 * client never blocks the broadcast to other players.
 */
public interface ClientCallbackRemote extends Remote {

    /**
     * Delivers a complete game-state snapshot to the client.
     * Called after every model change that affects the visible game state.
     */
    void onState(GameStateDto dto) throws RemoteException;

    /**
     * Delivers an error message (e.g. rejected command, disconnected player).
     */
    void onError(String message) throws RemoteException;

    /**
     * Delivers a lobby waiting message of the form {@code "current:expected"}
     * while the lobby is not yet full.
     */
    void onWaiting(String raw) throws RemoteException;

    /**
     * Notifies the client that the game is over.
     *
     * @param raw comma-separated winner names, or an empty string if no one won
     */
    void onGameOver(String raw) throws RemoteException;
}
