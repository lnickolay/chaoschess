package com.chaoschess.backend.core.engine;

import com.chaoschess.backend.core.model.MovementRule;
import com.chaoschess.backend.core.model.Piece;
import com.chaoschess.backend.core.model.PieceType;
import com.chaoschess.backend.core.model.Square;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Component
public class MovePool {

    private static final int INITIAL_CAPACITY = 1024;

    private final Deque<Move> freeMoves;

    public static long currentMovesUsed = 0;
    public static long maxMovesUsed = 0;
    public static long movesReturned = 0;

    public MovePool() {
        this.freeMoves = new ArrayDeque<>(INITIAL_CAPACITY);
        for (int i = 0; i < INITIAL_CAPACITY; i++) {
            this.freeMoves.push(new Move());
        }
    }

    public Move createMove(Square from, Square to, Piece movingPiece, PieceType movingPieceType,
                           boolean movingPieceWasUnmoved, Piece capturedPiece, Square capturedPieceLoc,
                           boolean capturedPieceWasUnmoved, boolean isCastling, Square newEnPassantMoveTarget,
                           Square newEnPassantCaptureTarget, PieceType promoPieceType, MovementRule movementRule) {
        Move move = borrowMove();
        move.setMove(from, to, movingPiece, movingPieceType, movingPieceWasUnmoved, capturedPiece, capturedPieceLoc,
                capturedPieceWasUnmoved, isCastling, newEnPassantMoveTarget, newEnPassantCaptureTarget, promoPieceType,
                movementRule);
        return move;
    }

    public Move cloneMove(Move move) {
        return createMove(
                move.getFrom(),
                move.getTo(),
                move.getMovingPiece(),
                move.getMovingPieceType(),
                move.getMovingPieceWasUnmoved(),
                move.getCapturedPiece(),
                move.getCapturedPieceLoc(),
                move.getCapturedPieceWasUnmoved(),
                move.getIsCastling(),
                move.getNewEnPassantMoveTarget(),
                move.getNewEnPassantCaptureTarget(),
                move.getPromoPieceType(),
                move.getMovementRule()
        );
    }

    public void releaseAllMoves(List<Move> moves) {
        moves.forEach(this::releaseMove);
    }

    public synchronized void releaseMove(Move move) {
        currentMovesUsed--;
        movesReturned++;

        move.reset();

        // Push only if initial capacity is not exceeded (optional, maintains the pool at a manageable size)
        if (this.freeMoves.size() < INITIAL_CAPACITY) {
            this.freeMoves.push(move);
        }
    }

    private synchronized Move borrowMove() {
        currentMovesUsed++;
        maxMovesUsed = Math.max(maxMovesUsed, currentMovesUsed);
        if (!this.freeMoves.isEmpty()) {
            return this.freeMoves.pop();
        } else {
            return new Move();
        }
    }
}
