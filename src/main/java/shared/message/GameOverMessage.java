package shared.message;

import shared.dto.event.EndGameScoringDto;

import java.util.List;

/**
 * Game-over notification carrying the winners and (optionally) the
 * end-game scoring breakdown.
 * <p>
 * {@code scoring} is {@code null} in forced-shutdown cases where there is no
 * real final computation (e.g. a suspension timeout that declares the only
 * connected player the winner by default).
 *
 * @param winners names of the winning players
 * @param scoring final-score breakdown, or {@code null} in forced shutdowns
 *                with no final computation
 */
public record GameOverMessage(List<String> winners, EndGameScoringDto scoring) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
