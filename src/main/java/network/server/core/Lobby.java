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

    /** Unique lobby id (a random UUID). */
    private final String id;
    /** Human-readable lobby name. */
    private final String name;
    /** Number of players that fills the lobby and starts the game. */
    private final int maxPlayers;
    /** Members currently in the lobby, in join order. */
    private final List<PlayerEntry> players = new ArrayList<>();

    /**
     * @param name the lobby's display name
     * @param maxPlayers the size that, once reached, starts the game
     */
    public Lobby(String name, int maxPlayers) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.maxPlayers = maxPlayers;
    }

    /**
     * Adds a player and broadcasts the updated state to the members.
     *
     * @param entry the player to add
     */
    public void addPlayer(PlayerEntry entry) {
        players.add(entry);
        broadcastState();
    }

    /**
     * Removes the given player (by identity). Does not broadcast.
     *
     * @param entry the player to remove
     */
    public void removePlayer(PlayerEntry entry) {
        players.remove(entry);
    }

    /** @return {@code true} if the lobby has reached its maximum size */
    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    /** @return a defensive copy of the current members */
    public List<PlayerEntry> getPlayers() {
        return new ArrayList<>(players);
    }

    /** @return this lobby's unique id */
    public String getId() {
        return id;
    }

    /** @return a {@link LobbyDto} snapshot of this lobby */
    public LobbyDto toDto() {
        return new LobbyDto(id, name, players.size(), maxPlayers);
    }

    /** @return the outbound views of all current members */
    public List<VirtualView> getViews() {
        return players.stream().map(PlayerEntry::getView).toList();
    }

    /** @return {@code true} if a player with the given name is in this lobby */
    public boolean containsPlayer(String playerName) {
        return players.stream().anyMatch(p -> p.getName().equals(playerName));
    }

    /** @return {@code true} if the lobby has no players left */
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

    /** Sends the current {@link LobbyDto} to every member's view. */
    public void broadcastState() {
        LobbyDto dto = toDto();
        getViews().forEach(v -> v.sendLobbyState(dto));
    }

    /** Notifies every member's view that the game is starting. */
    public void notifyGameStarting() {
        getViews().forEach(VirtualView::sendGameStarting);
    }
}
