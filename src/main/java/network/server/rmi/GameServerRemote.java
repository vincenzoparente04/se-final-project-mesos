package network.server.rmi;

import network.client.rmi.ClientCallbackRemote;
import shared.command.ClientCommand;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameServerRemote extends Remote {

    void join(String playerName, ClientCallbackRemote callback) throws RemoteException;

    void submitClientCommand(ClientCommand command) throws RemoteException;
}
