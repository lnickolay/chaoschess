package com.chaoschess.backend.core.model;

import java.util.Set;

public record MovementRule(int dx, int dy, int minSteps, int maxSteps, Set<MovementModifier> modifiers) {

    public boolean hasModifier(MovementModifier modifier) {
        return this.modifiers.contains(modifier);
    }
}
