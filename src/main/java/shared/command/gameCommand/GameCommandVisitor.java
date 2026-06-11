package shared.command.gameCommand;

/**
 * Visitor over the subtypes of {@link GameCommand}. Implemented by the
 * {@code GamePhaseHandler}s: each phase overrides only the relevant commands and
 * leaves the others to the default {@code throw IllegalStateException} defined
 * in {@code GamePhaseHandler}.
 */
public interface GameCommandVisitor {
    /**
     * @param command colour choice during setup
     * @throws Exception if the command is not allowed in the current phase or its application fails
     */
    void visit(ChooseColorCommand command) throws Exception;

    /**
     * @param command totem placement on an offer tile
     * @throws Exception if the command is not allowed in the current phase or its application fails
     */
    void visit(PlaceTotemCommand command) throws Exception;

    /**
     * @param command draw/selection of a card from the offer
     * @throws Exception if the command is not allowed in the current phase or its application fails
     */
    void visit(DrawCardCommand command) throws Exception;

    /**
     * @param command end of the current player's turn
     * @throws Exception if the command is not allowed in the current phase or its application fails
     */
    void visit(EndTurnCommand command) throws Exception;
}
