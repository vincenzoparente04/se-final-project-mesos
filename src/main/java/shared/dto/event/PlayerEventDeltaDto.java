package shared.dto.event;

import java.io.Serializable;

/**
 * Per-player delta produced by a single event resolution. Contains the
 * food/prestige snapshots before and after applying the event (so the
 * client can render absolute values, deltas, or both) and a free-form
 * {@code details} string with the human-readable explanation.
 */
public class PlayerEventDeltaDto implements Serializable {
    public final String playerName;
    public final int foodBefore;
    public final int foodAfter;
    public final int prestigeBefore;
    public final int prestigeAfter;
    public final String details;       // e.g. "2 hunters → +6 food, +4 prestige"

    public PlayerEventDeltaDto(String playerName,
                               int foodBefore, int foodAfter,
                               int prestigeBefore, int prestigeAfter,
                               String details) {
        this.playerName = playerName;
        this.foodBefore = foodBefore;
        this.foodAfter = foodAfter;
        this.prestigeBefore = prestigeBefore;
        this.prestigeAfter = prestigeAfter;
        this.details = details;
    }
}
