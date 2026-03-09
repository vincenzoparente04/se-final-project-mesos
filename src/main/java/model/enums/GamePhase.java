package model.enums;

public enum GamePhase {
    PLACEMENT,      // player placement on the OfferTrack, in turn order
    ACTION,         // players resolve actions in turn order on the OfferTrack
    END_OF_ROUND,   // events are resolved, rows are reorganized
    END_OF_GAME     // final score calculation
}
