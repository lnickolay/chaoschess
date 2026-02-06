package com.chaoschess.backend.core.model;

public record GameOutcome(GameOutcomeState state, Color winner) {

    public GameOutcomeCategory getCategory() {
        return this.state.getCategory();
    }
}
