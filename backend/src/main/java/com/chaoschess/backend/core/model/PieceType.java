package com.chaoschess.backend.core.model;

import java.util.Set;

public record PieceType(int id, String name, String symbol, Set<MovementRule> movementRules, PieceRole role) {}
