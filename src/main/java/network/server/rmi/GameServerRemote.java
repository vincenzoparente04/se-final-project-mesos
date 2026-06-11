package network.server.rmi;

import network.client.rmi.ClientCallbackRemote;
import shared.command.ClientCommand;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface exported by the server to RMI clients. A client looks it up in
 * the registry and uses it to register ({@link #join}), to forward its commands
 * ({@link #submitClientCommand}) and to announce a clean disconnect
 * ({@link #disconnect}).
 */
public interface GameServerRemote extends Remote {

    /**
     * Registers a player with the server. Returns {@code true} when the name
     * is accepted and the callback has been wired into the lobby; returns
     * {@code false} when another client is already connected with the same
     * name (the client may retry by calling {@code join} again with a
     * different name on the same stub).
     *
     * @param playerName the requested player name
     * @param callback the client's exported callback for server→client messages
     * @param host the client's advertised host, used for logging
     * @return {@code true} if the name was accepted, {@code false} if already taken
     * @throws RemoteException on RMI transport failure
     */
    boolean join(String playerName, ClientCallbackRemote callback, String host) throws RemoteException;

    /**
     * Submits a command from the client to the server-side dispatcher.
     * The command is routed based on its type: lobby commands go to the
     * {@link network.server.core.LobbyManager}, game commands go to the
     * active game queue, and heartbeat commands update the liveness sentinel.
     *
     * @param command the command to route
     * @throws RemoteException on RMI transport failure or a routing error
     */
    void submitClientCommand(ClientCommand command) throws RemoteException;

    /**
     * Announces a clean, client-initiated disconnect, triggering server-side teardown.
     *
     * @param playerName the disconnecting player's name
     * @throws RemoteException on RMI transport failure
     */
    void disconnect(String playerName) throws RemoteException;
}
