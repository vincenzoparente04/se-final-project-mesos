package network.client.rmi;

import database.ScoreRecord;
import shared.dto.GameStateDto;
import shared.dto.LobbyDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI callback exported by the client and invoked by the server to push messages
 * to it (the RMI counterpart of the socket {@code ServerMessage} stream). The
 * server issues these calls serially per client through {@code RmiVirtualView}'s
 * single-thread sender, so a client never sees overlapping callbacks.
 */
public interface ClientCallbackRemote extends Remote {

    /**
     * Delivers the latest game-state snapshot.
     * @param dto the game-state snapshot
     * @throws RemoteException on RMI transport failure
     */
    void onState(GameStateDto dto) throws RemoteException;

    /**
     * Delivers a textual error or status notification.
     * @param message the protocol-defined error/status message
     * @throws RemoteException on RMI transport failure
     */
    void onError(String message) throws RemoteException;

    /**
     * Delivers the list of open lobbies (lobby-browser view).
     * @param lobbies the lobbies currently joinable
     * @throws RemoteException on RMI transport failure
     */
    void onLobbyList(List<LobbyDto> lobbies) throws RemoteException;

    /**
     * Delivers the state of the lobby the player belongs to.
     * @param lobby the snapshot of the player's lobby
     * @throws RemoteException on RMI transport failure
     */
    void onLobbyState(LobbyDto lobby) throws RemoteException;

    /**
     * Notifies that the lobby has filled up and the game is starting.
     * @throws RemoteException on RMI transport failure
     */
    void onGameStarting() throws RemoteException;

    /**
     * Game-over notification. {@code scoring} is {@code null} when the
     * end-game scoring breakdown is not applicable (e.g. suspension-timeout
     * forfeit).
     * @param winners the winning player names (may be empty on a forfeit)
     * @param scoring the end-game scoring breakdown, or {@code null} if not applicable
     * @throws RemoteException on RMI transport failure
     */
    void onGameOver(List<String> winners, EndGameScoringDto scoring) throws RemoteException;

    /**
     * Delivers the post-game leaderboard and this player's placement.
     * @param leaderboard the top score records
     * @param rankPosition this player's 1-based position in the ranking
     * @param points this player's final score
     * @throws RemoteException on RMI transport failure
     */
    void onLeaderboardUpdate(List<ScoreRecord> leaderboard, int rankPosition, int points) throws RemoteException;

    /**
     * One-shot notification of a resolved event card (end-of-round / end-of-game).
     * @param resolution the resolved-event payload to display
     * @throws RemoteException on RMI transport failure
     */
    void onEventResolved(EventResolutionDto resolution) throws RemoteException;

    /**
     * Server→client heartbeat. The receiver only refreshes its liveness sentinel.
     * @throws RemoteException on RMI transport failure
     */
    void onHeartbeat() throws RemoteException;
}
