package com.chaoschess.backend.core.model;

public enum GameOutcomeState {
    ONGOING                 (GameOutcomeCategory.ONGOING),
    CHECKMATE               (GameOutcomeCategory.WIN_LOSS),
    RESIGNATION             (GameOutcomeCategory.WIN_LOSS),     // TODO
    TIME_LOSS               (GameOutcomeCategory.WIN_LOSS),     // TODO
    STALEMATE               (GameOutcomeCategory.DRAW),
    THREEFOLD_REPETITION    (GameOutcomeCategory.DRAW),         // TODO
    FIFTY_MOVE_RULE         (GameOutcomeCategory.DRAW),
    INSUFFICIENT_MATERIAL   (GameOutcomeCategory.DRAW),         // TODO
    AGREEMENT               (GameOutcomeCategory.DRAW);         // TODO

    private final GameOutcomeCategory category;

    GameOutcomeState(GameOutcomeCategory category) {
        this.category = category;
    }

    public GameOutcomeCategory getCategory() {
        return this.category;
    }
}
