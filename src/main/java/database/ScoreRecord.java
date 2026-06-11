package database;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents an immutable data transfer object (DTO) containing a player's match result.
 * 
 * This Java record safely transports player performance data from the game logic to the persistence layer.
 * It is immutable and implements {@link Serializable} for potential network transmission.
 *
 * @param nickname the unique username of the player
 * @param score the final score achieved by the player in the match
 * @param playerCount the total number of players who participated in the match
 * @param date the timestamp when the match concluded, used for sorting and historical records
 */
public record ScoreRecord(String nickname, int score, int playerCount, LocalDateTime date) implements Serializable {}
