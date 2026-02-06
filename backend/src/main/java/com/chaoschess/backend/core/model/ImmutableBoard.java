package com.chaoschess.backend.core.model;

import java.util.Map;
import java.util.Set;

public record ImmutableBoard(
        int width,
        int height,
        Piece[][] pieceGrid,
        Map<Color, Set<Square>> castlingPartnerLocs,
        Set<PieceType> promoOptions,
        Color colorToMove,
        Square enPassantMoveTarget,
        int halfmoveClock,
        int fullmoveNumber
) {

    // TODO: This method exists here and in Board, remove it here or there
    public Piece getPieceAt(int x, int y) {
        return this.pieceGrid[x][y];
    }

    // TODO: This method exists here and in Board, remove it here or there
    public Piece getPieceAt(Square square) {
        return getPieceAt(square.x(), square.y());
    }
}
