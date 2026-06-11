package shared.dto;

import java.io.Serializable;

/**
 * Serializable view of a single slot in the turn-order track: its 0-based
 * position and the occupant, or {@code null} when the slot is free.
 */
public class TurnOrderSlotDto implements Serializable {
    public final int position;        // 0-based index
    public final String occupantName; // null if the slot is free

    public TurnOrderSlotDto(int position, String occupantName) {
        this.position = position;
        this.occupantName = occupantName;
    }
}
