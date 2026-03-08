package controller.endOfRound;

import model.GameModel;

public class EndOfRoundManager {

    // REFACTOR: VA AGGIUNTA GESTIONE DEGLI EFFETTI DEGLI EDIFICI CHE NON SONO LEGATI AGLI EVENTI
    // NON TUTTI SONO A FINE ROUND: DA MODIFICARE ANCHE GLI ALTRI MANAGER
    private final GameModel model;
    private final EventResolver eventResolver;

    public void resolveEndOfRound()
    // coordinates the 6 steps in the following order:
    // 1. resolveEvents()
    // 2. discardBottomRow()
    // 3. moveTopRowToBottom()
    // 4. restoreTopRow()
    // 5. checkNewEra()
    // 6. checkGameOver() → handled by GameController

    private void resolveEvents()
    // reads events from BottomRow:
    //   List<EventCard> events = bottomRow.getEvents()
    // list them putting soutenance last
    // for each event: eventResolver.resolve(event, model)

    private void discardBottomRow()
    // bottomRow.discardTribeCards()
    // building cards remain in the bottom row (unless new era is revealed), while tribe cards are discarded

    private void moveTopRowToBottom()
    // List<TribeCard> cards = topRow.extractTribeCardsForBottomRow()
    // cards.forEach(card -> bottomRow.addTribeCard(card))

    private void restoreTopRow()
    // int target = model.getPlayerCount() + 4
    // draw cards until it reaches target
    // the EventCard don't count towards the target but go to the TopRow
    // delegate to TribeDeck.draw() and TopRow.addTribeCard()

    private void checkNewEra()
    // if (model.getTribeDeck().isNewEraRevealed()):
    //   updates model.setCurrentEra(newEra)
    //   handles the BuildingCard:
    //     if Era III: discards BuildingCard from BottomRow
    //     moves BuildingCard to TopRow from BottomRow
    //     adds new BuildingCard to TopRow from BuildingDeck
}