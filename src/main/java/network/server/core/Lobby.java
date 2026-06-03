package network.server.core;

import shared.dto.LobbyDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A pre-game gathering of {@link PlayerEntry}s waiting for the lobby to fill
 * up.
 *
 * <h2>Thread safety</h2>
 * This class is <strong>not</strong> internally synchronized. All mutators
 * must be invoked while holding {@code LobbyManager.this}'s monitor — i.e.
 * from within a {@code synchronized} method of {@link LobbyManager}, which is
 * the sole owner of every {@code Lobby} instance.
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

    public void addPlayer(PlayerEntry entry) {
        players.add(entry);
        broadcastState();
    }

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

    /** True se c'è un player con quel nome in questa lobby. */
    public boolean containsPlayer(String playerName) {
        return players.stream().anyMatch(p -> p.getName().equals(playerName));
    }

    /** True se la lobby non ha più giocatori. */
    public boolean isEmpty() {
        return players.isEmpty();
    }

    /**
     * Rimuove dalla lobby il player con il nome dato.
     * @return true se il player era presente ed è stato rimosso, false altrimenti
     */
    public void removePlayerByName(String playerName) {
        players.removeIf(p -> p.getName().equals(playerName));
        broadcastState();
    }

    /** Invia il LobbyDto corrente a tutte le view dei player di questa lobby. */
    public void broadcastState() {
        LobbyDto dto = toDto();
        getViews().forEach(v -> v.sendLobbyState(dto));
    }

    /** Invia sendGameStarting a tutte le view dei player di questa lobby. */
    public void notifyGameStarting() {
        getViews().forEach(VirtualView::sendGameStarting);
    }
}
