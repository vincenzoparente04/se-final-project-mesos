package shared.dto;

import java.io.Serializable;

/**
 * Serializable view of a lobby shown in the menu (lobby list / lobby state).
 *
 * @param id             unique lobby identifier
 * @param name           display name of the lobby
 * @param currentPlayers number of players currently in the lobby
 * @param maxPlayers     maximum number of players for the game
 */
public record LobbyDto(String id, String name, int currentPlayers, int maxPlayers)
        implements Serializable {}
