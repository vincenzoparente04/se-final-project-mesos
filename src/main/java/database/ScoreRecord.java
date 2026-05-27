package database;

import java.time.LocalDateTime;

/**
 * Represents an immutable data transfer object (DTO) containing a player's match result.
 * This record safely transports data from the game logic to the persistence layer.
 * @param nickname    The unique username of the player.
 * @param score       The final score achieved by the player in the match.
 * @param playerCount The total number of players who participated in the match,
 * which affects the weight of the final score.
 * @param date        The timestamp when the match concluded, used for sorting and historical records.
 * @implNote Java Record (Immutable DTO)
 */
public record ScoreRecord(String nickname, int score, int playerCount, LocalDateTime date) {}
