package shared.dto.event;

import java.io.Serializable;
import java.util.List;

/**
 * End-of-game scoring snapshot. Contains, for every player, the breakdown
 * of the five end-game scoring sources (Builders, Artists, Inventors,
 * Buildings printed PP, EndGame building effects) plus the prestige
 * before/after. Shipped together with the winners inside the
 * {@code GameOverMessage}.
 */
public class EndGameScoringDto implements Serializable {
    public final List<PlayerScoringDeltaDto> deltas;

    public EndGameScoringDto(List<PlayerScoringDeltaDto> deltas) {
        this.deltas = deltas;
    }
}
