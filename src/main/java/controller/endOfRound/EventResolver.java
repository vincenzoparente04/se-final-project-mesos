package controller.endOfRound;

import model.GameModel;
import model.cards.EventCard;

public class EventResolver {

    // REFACTOR: ATTENZIONE AL CONTROLLO SUGLI EFFETTI DEI BUILDING QUANDO SI RISOLVONO GLI EVENTI
    private final GameModel model;

    public void resolve(EventCard event)
    // REFACTOR: SWITCH BRUTTO VA CAMBIATO, DA RIVEDERE ANCHE LA GESTIONE DEI TIPI EVENTO
    switch (event.getEventType()) {
        case HUNT            -> resolveHunt(event)
        case SHAMANIC_RITUAL -> resolveShamanicRitual(event)
        case CAVE_PAINTINGS  -> resolveCavePaintings(event)
        case SUSTENANCE      -> resolveSustenance(event)
    }

    // REFACTOR: CONTROLLA IL DISCORSO CON handleImmediateEffCT
    private void resolveHunt(EventCard event)
    // for every player:
    //   int hunters = player.getTribe().countByType(HUNTER)
    //   player.addFood(hunters)
    //   player.addPrestigePoints(hunters * event.getPrimaryValue())
    // Tribe.countByType() knows how many Hunter cards the player has, and the event gives 1 Food and some PP for each Hunter card

    private void resolveShamanicRitual(EventCard event)
    // for every player:
    //   int stars = player.getTribe().getTotalShamanStars()
    // find who has the maximum and who has the minimum
    // in case of a tie, all tied players gain/lose
    // player with max: addPrestigePoints(event.getPrimaryValue())
    // player with min: removePrestigePoints(event.getPrimaryValue())

    private void resolveCavePaintings(EventCard event)
    // event.getPrimaryValue() = soglia sotto cui si perde
    // event.getSecondaryValue() = PP per Artist se sopra soglia
    // for every player:
    //   int artists = player.getTribe().countByType(ARTIST)
    //   if artists < event.getPrimaryValue():
    //     player.removePrestigePoints(event.getSecondaryValue())
    //   else:
    //     player.addPrestigePoints(artists * event.getSecondaryValue())

    private void resolveSustenance(EventCard event)
    // for every player:
    //   int characters = player.getTribe().getTotalCharacterCount()
    //   int discount = player.getTribe().countGatherers() * 3
    //   int foodToPay = Math.max(0, characters - discount)
    //   if player.hasFood(foodToPay):
    //     player.removeFood(foodToPay)
    //   else:
    //     int paid = player.getFood()
    //     int unpaid = foodToPay - paid
    //     player.removeFood(paid)
    //     player.removePrestigePoints(unpaid * event.getPrimaryValue())
