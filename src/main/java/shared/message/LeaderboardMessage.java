package shared.message;

import database.ScoreRecord;

import java.util.List;

public record LeaderboardMessage(List<ScoreRecord> leaderboard, int rankPosition, int points) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
