package model.enums;

public enum GamePhase {
    SETUP,                  // board setup, turn order randomization, starting resource distribution
    COLOR_CHOOSING_PHASE,   // players choose their totem colors in turn order
    PLACEMENT,              // player placement on the OfferTrack, in turn order
    ACTION,                 // players resolve actions in turn order on the OfferTrack
    PRE_END_OF_ROUND,
    END_OF_ROUND,           // events are resolved, rows are reorganized
    END_OF_GAME             // final score calculation
}
