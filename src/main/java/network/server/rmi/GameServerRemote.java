package network.server.rmi;

import network.client.rmi.ClientCallbackRemote;
import shared.command.ClientCommand;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI remote interface exposed by the server. Clients obtain a stub for this
 * interface from the RMI registry and use it to join a lobby, submit commands,
 * and disconnect gracefully.
 *
 * @see GameServerRemoteImpl
 */
public interface GameServerRemote extends Remote {

    /**
     * Registers a player with the server. Returns {@code true} when the name
     * is accepted and the callback has been wired into the lobby; returns
     * {@code false} when another client is already connected with the same
     * name (the client may retry by calling {@code join} again with a
     * different name on the same stub).
     *
     * @param playerName the name the player wants to use
     * @param callback   the client-side stub the server will use to push messages
     * @param host       the client's host address, used for logging
     * @return {@code true} if the registration was accepted, {@code false} otherwise
     * @throws RemoteException if the RMI call fails
     */
    boolean join(String playerName, ClientCallbackRemote callback, String host) throws RemoteException;

    /**
     * Submits a command from the client to the server-side dispatcher.
     * The command is routed based on its type: lobby commands go to the
     * {@link network.server.core.LobbyManager}, game commands go to the
     * active game queue, and heartbeat commands update the liveness sentinel.
     *
     * @param command the command to submit
     * @throws RemoteException if the RMI call fails or command routing throws
     */
    void submitClientCommand(ClientCommand command) throws RemoteException;

    /**
     * Notifies the server that the player is disconnecting gracefully.
     * Triggers the same disconnect pipeline as an unintentional drop.
     *
     * @param playerName the name of the disconnecting player
     * @throws RemoteException if the RMI call fails
     */
    void disconnect(String playerName) throws RemoteException;
}
