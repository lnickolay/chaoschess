package com.chaoschess.backend.core.engine;

import com.chaoschess.backend.core.model.MovementRule;
import com.chaoschess.backend.core.model.Piece;
import com.chaoschess.backend.core.model.PieceType;
import com.chaoschess.backend.core.model.Square;

public final class Move {

    public static long instanceCounter = 0;

    private Square from;
    private Square to;
    private Piece movingPiece;
    private PieceType movingPieceType;
    private boolean movingPieceWasUnmoved;
    private Piece capturedPiece;
    private Square capturedPieceLoc;
    private boolean capturedPieceWasUnmoved;
    private boolean isCastling;
    private Square newEnPassantMoveTarget;
    private Square newEnPassantCaptureTarget;
    private PieceType promoPieceType;
    private MovementRule movementRule;

    public Move() {
        reset();
        instanceCounter++;
    }

    public void reset() {
        this.from = null;
        this.to = null;
        this.movingPiece = null;
        this.movingPieceType = null;
        this.movingPieceWasUnmoved = false;
        this.capturedPiece = null;
        this.capturedPieceLoc = null;
        this.capturedPieceWasUnmoved = false;
        this.isCastling = false;
        this.newEnPassantMoveTarget = null;
        this.newEnPassantCaptureTarget = null;
        this.promoPieceType = null;
        this.movementRule = null;
    }

    public void setMove(Square from, Square to, Piece movingPiece, PieceType movingPieceType,
                        boolean movingPieceWasUnmoved, Piece capturedPiece, Square capturedPieceLoc,
                        boolean capturedPieceWasUnmoved, boolean isCastling, Square newEnPassantMoveTarget,
                        Square newEnPassantCaptureTarget, PieceType promoPieceType, MovementRule movementRule) {
        this.from = from;
        this.to = to;
        this.movingPiece = movingPiece;
        this.movingPieceType = movingPieceType;
        this.movingPieceWasUnmoved = movingPieceWasUnmoved;
        this.capturedPiece = capturedPiece;
        this.capturedPieceLoc = capturedPieceLoc;
        this.capturedPieceWasUnmoved = capturedPieceWasUnmoved;
        this.isCastling = isCastling;
        this.newEnPassantMoveTarget = newEnPassantMoveTarget;
        this.newEnPassantCaptureTarget = newEnPassantCaptureTarget;
        this.promoPieceType = promoPieceType;
        this.movementRule = movementRule;
    }

    public Square getFrom() { return this.from; }
    public Square getTo() { return this.to; }
    public Piece getMovingPiece() { return this.movingPiece; }
    public PieceType getMovingPieceType() { return this.movingPieceType; }
    public boolean getMovingPieceWasUnmoved() { return this.movingPieceWasUnmoved; }
    public Piece getCapturedPiece() { return this.capturedPiece; }
    public Square getCapturedPieceLoc() { return this.capturedPieceLoc; }
    public boolean getCapturedPieceWasUnmoved() { return this.capturedPieceWasUnmoved; }
    public boolean getIsCastling() { return this.isCastling; }
    public Square getNewEnPassantMoveTarget() { return this.newEnPassantMoveTarget; }
    public Square getNewEnPassantCaptureTarget() { return this.newEnPassantCaptureTarget; }
    public PieceType getPromoPieceType() { return this.promoPieceType; }
    public MovementRule getMovementRule() { return this.movementRule; }

    public boolean isCapture() {
        return this.capturedPiece != null;
    }

    public boolean isPromo() {
        return this.promoPieceType != null;
    }
}
