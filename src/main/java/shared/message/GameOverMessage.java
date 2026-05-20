package shared.message;

import shared.dto.event.EndGameScoringDto;

import java.util.List;

/**
 * Game-over notification carrying the winners and (optionally) the
 * end-game scoring breakdown.
 * <p>
 * {@code scoring} è {@code null} nei casi di chiusura forzata in cui non
 * c'è un vero calcolo finale (es. timeout di sospensione che proclama
 * vincitore d'ufficio l'unico player connesso).
 */
public record GameOverMessage(List<String> winners, EndGameScoringDto scoring) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
