package com.chaoschess.backend.core.ai;

import com.chaoschess.backend.core.model.PieceType;

import java.util.Map;

public record PSTData(Map<PieceType, int[][]> midgamePSTs, Map<PieceType, int[][]> endgamePSTs) {}
