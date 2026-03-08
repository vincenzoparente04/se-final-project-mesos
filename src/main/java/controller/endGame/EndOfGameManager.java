package controller.endGame;

import model.GameModel;
import view.GameView;

public class EndOfGameManager {

    private final GameModel model;
    private final GameView view;

    public void resolveEndOfGame()
    // 1. resolves all visible events including those in TopRow
    //    (special rule only for the last round)
    // 2. calculates the final points for each player
    // 3. determines the winner
    // 4. communicates the result to the View

    private void resolveFinalEvents()
    // resolves before events from top row, if any, then bottom row events

    // CHIARAMENTE DA RIFARE MEGLIO E CON PIU DETTAGLI, MA L'IDEA GENERALE E' QUESTA:
    private void calculateFinalPoints()
    // for every player:
    //   calculate PP from Builder:
    //     tribe.getByType(BUILDER)
    //       .forEach(b -> player.addPrestigePoints(b.calculateEndGamePoints(tribe)))
    //
    //   calcola PP da Artist (una sola volta, non per carta):
    //     int artistPP = new ArtistCard().calculateEndGamePoints(tribe)
    //     player.addPrestigePoints(artistPP)
    //
    //   calcola PP da Inventor (una sola volta):
    //     int inventorPP = new InventorCard().calculateEndGamePoints(tribe)
    //     player.addPrestigePoints(inventorPP)
    //
    //   calcola PP da Building:
    //     tribe.getAllBuildings()
    //       .forEach(b -> player.addPrestigePoints(b.getEndGamePoints()))
    //     + applies the special effects of the BuildingCard if they give PP at the end of the game

    private Player determineWinner()
    // finds player with most PP
    // if is tie: wins who has more food
    // if is tie again is a draw
}