package network.server.core;

import shared.dto.LobbyDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public List<PlayerEntry> getPlayers() {
        return List.copyOf(players);
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
}
