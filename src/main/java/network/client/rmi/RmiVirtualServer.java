package client.rmi;

import client.ClientStateListener;
import client.LocalGameState;
import client.VirtualServer;
import shared.command.ChooseColorCommand;
import shared.command.DrawCardCommand;
import shared.command.EndTurnCommand;
import shared.command.GameCommand;
import shared.command.PlaceTotemCommand;
import shared.rmi.GameServerRemote;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * {@link VirtualServer} implementation that communicates with the server via RMI.
 * <p>
 * On construction (blocking — must be called off the UI thread):
 * <ol>
 *   <li>Looks up {@code "MesosGameServer"} in the RMI registry.</li>
 *   <li>Creates and exports a {@link ClientCallbackImpl} so the server can
 *       push state updates back.</li>
 *   <li>Calls {@link GameServerRemote#join} to register in the lobby.</li>
 * </ol>
 * Each {@code send*} method submits the corresponding {@link GameCommand} to
 * a single-threaded executor, keeping the UI thread unblocked.
 * <p>
 * {@link #close()} shuts down the command executor <em>and</em> un-exports
 * the callback stub so the JVM can exit cleanly.
 */
public class RmiVirtualServer implements VirtualServer {

    private static final String SERVICE_NAME = "MesosGameServer";

    private final String playerName;
    private final GameServerRemote serverStub;
    private final ClientCallbackImpl callback;
    private final ExecutorService commandExecutor;

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

        serverStub.join(playerName, callback);
    }

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

    /**
     * Shuts down the command executor (waiting up to 1 s for in-flight commands)
     * and un-exports the callback stub so the JVM can exit cleanly.
     */
    @Override
    public void close() {
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

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private void submitAsync(GameCommand command) {
        commandExecutor.submit(() -> {
            try {
                serverStub.submitCommand(command);
            } catch (RemoteException e) {
                System.err.println("RMI command failed: " + e.getMessage());
            }
        });
    }
}
