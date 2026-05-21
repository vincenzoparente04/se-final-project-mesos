package network.server.rmi;

import network.client.rmi.ClientCallbackRemote;
import shared.command.ClientCommand;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameServerRemote extends Remote {

    /**
     * Registers a player with the server. Returns {@code true} when the name
     * is accepted and the callback has been wired into the lobby; returns
     * {@code false} when another client is already connected with the same
     * name (the client may retry by calling {@code join} again with a
     * different name on the same stub).
     */
    boolean join(String playerName, ClientCallbackRemote callback, String host) throws RemoteException;

    void submitClientCommand(ClientCommand command) throws RemoteException;

    // per disconnettersi
    void disconnect(String playerName) throws RemoteException;
}
