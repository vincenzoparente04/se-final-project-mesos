package shared.command;

public interface CommandVisitor {
    void visit(ChooseColorCommand command) throws Exception;
    void visit(PlaceTotemCommand command) throws Exception;
    void visit(DrawCardCommand command) throws Exception;
    void visit(EndTurnCommand command) throws Exception;
}
