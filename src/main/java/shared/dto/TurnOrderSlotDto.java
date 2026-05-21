package shared.dto;

import java.io.Serializable;

public class TurnOrderSlotDto implements Serializable {
    public final int position;        // 0-based index
    public final String occupantName; // null if the slot is free

    public TurnOrderSlotDto(int position, String occupantName) {
        this.position = position;
        this.occupantName = occupantName;
    }
}
