package shared.dto;

import java.io.Serializable;

public record LobbyDto(String id, String name, int currentPlayers, int maxPlayers)
        implements Serializable {}
