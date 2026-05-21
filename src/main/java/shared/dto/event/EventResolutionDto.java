package shared.dto.event;

import java.io.Serializable;
import java.util.List;

/**
 * Snapshot of one event card resolution. The server emits one of these for
 * every event card processed at the end of a round (or at end-of-game).
 * The {@code details} stringhe sui delta dei singoli player sono già
 * formattate server-side: il client le mostra come sono.
 */
public class EventResolutionDto implements Serializable {
    public final String eventType;     // EventType.name(): HUNT / CAVE_PAINTINGS / SHAMANIC_RITUAL / SUSTENANCE / NASCONDINO
    public final String era;           // Era.name()
    public final int cardId;
    public final String headline;      // "Hunt event - Era II"
    public final List<PlayerEventDeltaDto> deltas;

    public EventResolutionDto(String eventType, String era, int cardId,
                              String headline, List<PlayerEventDeltaDto> deltas) {
        this.eventType = eventType;
        this.era = era;
        this.cardId = cardId;
        this.headline = headline;
        this.deltas = deltas;
    }
}
