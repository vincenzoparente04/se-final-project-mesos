package network.server.rmi;

import shared.command.ClientCommand;

import java.rmi.Remote;
import java.rmi.RemoteException;

import network.client.rmi.ClientCallbackRemote;

public interface GameServerRemote extends Remote {

    void join(String playerName, ClientCallbackRemote callback) throws RemoteException;

    void submitClientCommand(ClientCommand command) throws RemoteException;
}
