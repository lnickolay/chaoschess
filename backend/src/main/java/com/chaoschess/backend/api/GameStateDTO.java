package com.chaoschess.backend.api;

import com.chaoschess.backend.core.model.Color;
import com.chaoschess.backend.core.model.GameOutcome;

import java.util.List;
import java.util.Set;

public record GameStateDTO(
        int width,
        int height,
        Color colorToMove,
        PieceData[] pieceGrid,
        List<String> promoOptionNames,
        List<MoveOption> pseudolegalMoveOptions,
        GameOutcome gameOutcome,
        Set<Color> botColors
) {

    public record PieceData(String pieceName, Color color) {}
    public record MoveOption(int fromSquareIndex, int toSquareIndex, boolean isPromo, boolean isLegal) {}
}
