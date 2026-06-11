package network.server.core;

import shared.dto.LobbyDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A pre-game gathering of {@link PlayerEntry}s waiting for the lobby to fill up.
 * Not internally synchronized: {@link LobbyManager} is the sole owner and mutates
 * it only on its single lobby thread, so all access is already serialised.
 */
public class Lobby {

    private final String id;
    private final String name;
    private final int maxPlayers;
    private final List<PlayerEntry> players = new ArrayList<>();

    public Lobby(String name, int maxPlayers) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.maxPlayers = maxPlayers;
    }

    /**
     * Adds a player to the lobby and broadcasts the updated state to all members.
     *
     * @param entry the player to add
     */
    public void addPlayer(PlayerEntry entry) {
        players.add(entry);
        broadcastState();
    }

    /**
     * Removes a player from the lobby without broadcasting a state update.
     *
     * @param entry the player to remove
     */
    public void removePlayer(PlayerEntry entry) {
        players.remove(entry);
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public List<PlayerEntry> getPlayers() {
        return new ArrayList<>(players);
    }

    public String getId() {
        return id;
    }

    public LobbyDto toDto() {
        return new LobbyDto(id, name, players.size(), maxPlayers);
    }

    public List<VirtualView> getViews() {
        return players.stream().map(PlayerEntry::getView).toList();
    }

    /** True if the lobby contains that player */
    public boolean containsPlayer(String playerName) {
        return players.stream().anyMatch(p -> p.getName().equals(playerName));
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    /**
     * Removes the player with the given name and broadcasts the updated state.
     *
     * @param playerName the name of the player to remove
     */
    public void removePlayerByName(String playerName) {
        players.removeIf(p -> p.getName().equals(playerName));
        broadcastState();
    }

    /** Sends the current {@link shared.dto.LobbyDto} snapshot to all players in this lobby. */
    public void broadcastState() {
        LobbyDto dto = toDto();
        getViews().forEach(v -> v.sendLobbyState(dto));
    }

    /** Notifies all players in this lobby that the game is about to start. */
    public void notifyGameStarting() {
        getViews().forEach(VirtualView::sendGameStarting);
    }
}
