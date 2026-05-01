package network.client.rmi;

import shared.command.ChooseColorCommand;
import shared.command.ClientCommand;
import shared.command.CreateLobbyCommand;
import shared.command.DrawCardCommand;
import shared.command.EndTurnCommand;
import shared.command.JoinLobbyCommand;
import shared.command.ListLobbiesCommand;
import shared.command.PlaceTotemCommand;
import shared.command.HeartbeatCommand;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;

import network.client.LocalGameState;
import network.client.VirtualServer;
import network.client.ClientStateListener;
import network.server.rmi.GameServerRemote;

public class RmiVirtualServer implements VirtualServer {

    private static final String SERVICE_NAME = "MesosGameServer";

    private final String playerName;
    private final GameServerRemote serverStub;
    private final ClientCallbackImpl callback;
    private final ExecutorService commandExecutor;

    /** Periodo di invio heartbeat: deve essere < del timeout server (6s). */
    private static final long HEARTBEAT_INTERVAL_MS = 2_000L;
    private final ScheduledExecutorService heartbeatScheduler;

    public RmiVirtualServer(String host, int rmiPort, String playerName,
                            LocalGameState localState, ClientStateListener listener)
            throws Exception {
        this.playerName = playerName;

        Registry registry = LocateRegistry.getRegistry(host, rmiPort);
        this.serverStub = (GameServerRemote) registry.lookup(SERVICE_NAME);
        this.callback = new ClientCallbackImpl(localState, listener);

        this.commandExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rmi-commands-" + playerName);
            t.setDaemon(true);
            return t;
        });

        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "heartbeat-sender-" + playerName);
            t.setDaemon(true);
            return t;
        });

        serverStub.join(playerName, callback);

        this.heartbeatScheduler.scheduleAtFixedRate(
                () -> submitAsync(new HeartbeatCommand(playerName)),
                HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    // ─── Game commands ────────────────────────────────────────

    @Override
    public void sendChooseColor(String color) {
        submitAsync(new ChooseColorCommand(playerName, color));
    }

    @Override
    public void sendPlaceTotem(char tile) {
        submitAsync(new PlaceTotemCommand(playerName, tile));
    }

    @Override
    public void sendDrawCard(int cardId) {
        submitAsync(new DrawCardCommand(playerName, cardId));
    }

    @Override
    public void sendEndTurn() {
        submitAsync(new EndTurnCommand(playerName));
    }

    // ─── Lobby commands ───────────────────────────────────────

    @Override
    public void sendCreateLobby(int maxPlayers) {
        submitAsync(new CreateLobbyCommand(playerName, maxPlayers));
    }

    @Override
    public void sendJoinLobby(String lobbyId) {
        submitAsync(new JoinLobbyCommand(playerName, lobbyId));
    }

    @Override
    public void sendListLobbies() {
        submitAsync(new ListLobbiesCommand(playerName));
    }

    @Override
    public void close() {
        heartbeatScheduler.shutdownNow();


        // 1. Notifica il server della disconnessione
        try {
            serverStub.disconnect(playerName);
        } catch (RemoteException e) {
            // Ignorata: se il server è già caduto non è un problema
        }

        commandExecutor.shutdown();
        try {
            if (!commandExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                commandExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            commandExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        try {
            UnicastRemoteObject.unexportObject(callback, true);
        } catch (RemoteException ignored) {}
    }

    private void submitAsync(ClientCommand command) {
        commandExecutor.submit(() -> {
            try {
                serverStub.submitClientCommand(command);
            } catch (RemoteException e) {
                System.err.println("RMI command failed: " + e.getMessage());
            }
        });
    }
}
