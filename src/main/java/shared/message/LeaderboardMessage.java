package shared.message;

import database.ScoreRecord;

import java.util.List;

/**
 * Server→client message with the persisted leaderboard and the player's
 * placement at the end of the game.
 *
 * @param leaderboard  score records, ordered (global leaderboard)
 * @param rankPosition the player's position in the leaderboard
 * @param points       points scored by the player in the game
 */
public record LeaderboardMessage(List<ScoreRecord> leaderboard, int rankPosition, int points) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
