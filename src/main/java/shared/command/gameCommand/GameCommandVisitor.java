package shared.command.gameCommand;

/**
 * Visitor sui sotto-tipi di {@link GameCommand}. Implementato dai
 * {@code GamePhaseHandler}: ogni fase override solo i comandi pertinenti e
 * lascia agli altri il default {@code throw IllegalStateException} definito
 * in {@code GamePhaseHandler}.
 */
public interface GameCommandVisitor {
    void visit(ChooseColorCommand command) throws Exception;
    void visit(PlaceTotemCommand command) throws Exception;
    void visit(DrawCardCommand command) throws Exception;
    void visit(EndTurnCommand command) throws Exception;
}
