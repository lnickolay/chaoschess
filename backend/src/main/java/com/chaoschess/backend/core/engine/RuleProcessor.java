package com.chaoschess.backend.core.engine;

import com.chaoschess.backend.core.engine.board.Board;
import com.chaoschess.backend.core.model.Color;
import com.chaoschess.backend.core.model.GameOutcome;
import com.chaoschess.backend.core.model.GameOutcomeState;
import com.chaoschess.backend.core.model.MovementModifier;
import com.chaoschess.backend.core.model.MovementRule;
import com.chaoschess.backend.core.model.Piece;
import com.chaoschess.backend.core.model.PieceRole;
import com.chaoschess.backend.core.model.PieceType;
import com.chaoschess.backend.core.model.Square;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RuleProcessor {

    private final MovePool movePool;

    public RuleProcessor(MovePool movePool) {
        this.movePool = movePool;
    }

    public MovePool getMovePool() {
        return movePool;
    }

    public GameOutcome determineGameOutcome(Board board, List<Move> legalMoves) {
        Color colorToMove = board.getColorToMove();
        if (legalMoves.isEmpty()) {
            if (isKingInCheck(board, colorToMove)) {
                return new GameOutcome(GameOutcomeState.CHECKMATE, colorToMove.getOpponent());
            } else {
                return new GameOutcome(GameOutcomeState.STALEMATE, null);
            }
        }

        if (board.getHalfmoveClock() >= 50) {
            return new GameOutcome(GameOutcomeState.FIFTY_MOVE_RULE, null);
        }
        if (isDrawByThreefoldRepetition(board)) {
            return new GameOutcome(GameOutcomeState.THREEFOLD_REPETITION, null);
        }

        return new GameOutcome(GameOutcomeState.ONGOING, null);
    }

    private boolean isDrawByThreefoldRepetition(Board board) {
        long currentZobristHash = board.getZobristHash();
        List<Long> zobristHashHistory = board.getZobristHashHistory();
        int count = 0;

        for (int i = zobristHashHistory.size() - 1; i >= 0; i--) {
            if (zobristHashHistory.get(i).equals(currentZobristHash)) {
                count++;
            }
            // The zobrist hash of the current position is not part of the zobrist hash history yet, so the check needs
            // to be for count >= 2 and not count >= 3
            if (count >= 2) {
                return true;
            }
        }
        return false;
    }

    public List<Move> calculateLegalMoves(Board board) {
        List<Move> pseudolegalMoves = calculatePseudolegalMoves(board, board.getColorToMove());
        return calculateLegalMoves(board, pseudolegalMoves, true);
    }

    public List<Move> calculateLegalMoves(Board board, List<Move> pseudolegalMoves, boolean releaseNonLegalMoves) {
        List<Move> legalMoves = new ArrayList<>();

        for (Move move : pseudolegalMoves) {
            boolean isLegal = false;
            if (board.getColorToMove().equals(move.getMovingPiece().color())) {
                if (!move.getIsCastling()) {
                    board.makeMove(move);
                    if (!isKingInCheck(board, board.getColorToMove().getOpponent())) {
                        isLegal = true;
                    }
                    board.unmakeMove(move);
                } else {
                    if (isCastlingLegal(board, move)) {
                        isLegal = true;
                    }
                }
            }
            if (isLegal) {
                legalMoves.add(move);
            } else {
                if (releaseNonLegalMoves) {
                    // TODO: Should these be released here or maybe somewhere else?
                    getMovePool().releaseMove(move);
                }
            }
        }
        return legalMoves;
    }

    // TODO: Maybe different names for the next few methods instead of overloading? Or maybe remove some of them?
    public List<Move> calculatePseudolegalMoves(Board board) {
        List<Move> pseudolegalMoves = new ArrayList<>();
        for (Color color : Color.values()) {
            pseudolegalMoves.addAll(calculatePseudolegalMoves(board, color));
        }
        return pseudolegalMoves;
    }

    public List<Move> calculatePseudolegalMoves(Board board, Color color) {
        return calculatePseudolegalMoves(board, color, false, false);
    }

    private List<Move> calculatePseudolegalMoves(Board board, Color color, boolean stopOnFirstCheck,
                                                 boolean includeAttacksOnEmptySquares) {
        List<Move> pseudolegalMoves = new ArrayList<>();
        for (Square from : board.getPieceLocs().get(color)) {
            List<Move> pseudolegalMovesFromSquare = calculatePseudolegalMoves(board, from, stopOnFirstCheck,
                    includeAttacksOnEmptySquares);
            pseudolegalMoves.addAll(pseudolegalMovesFromSquare);

            // TODO: den stopOnFirstCheck-Kram weniger hässlich lösen
            if (stopOnFirstCheck && !pseudolegalMovesFromSquare.isEmpty()
                    && pseudolegalMovesFromSquare.getLast().getTo().equals(
                            board.getKingLocs().get(color.getOpponent()))) {
                return pseudolegalMoves;
            }
        }
        return pseudolegalMoves;
    }

    private List<Move> calculatePseudolegalMoves(Board board, Square from, boolean stopOnFirstCheck,
                                                 boolean includeAttacksOnEmptySquares) {
        List<Move> pseudolegalMovesFromSquare = new ArrayList<>();

        Piece movingPiece = board.getPieceAt(from);
        if (movingPiece == null) {
            return pseudolegalMovesFromSquare;
        }
        Color movingColor = movingPiece.color();
        PieceType movingPieceType = movingPiece.type();
        boolean movingPieceWasUnmoved = board.isUnmovedAt(from);

        Square prevEpCaptureTarget = board.getEnPassantCaptureTarget();

        int verticalSign = movingColor.equals(Color.WHITE) ? 1 : -1;
        for (MovementRule movementRule : movingPiece.type().movementRules()) {
            if (movementRule.hasModifier(MovementModifier.ONLY_UNMOVED) && !movingPieceWasUnmoved) {
                continue;
            }

            if (movementRule.hasModifier(MovementModifier.CASTLING)) {
                // TODO: Maybe move this check to isCastlingPseudolegal()?
                if (board.isKingUnmoved(movingColor)) {
                    // TODO: Unwanted side effect of adding castling moves directly to pseudo-legal moves: the king now
                    //  incorrectly appears to defend all still available castling-partner rooks
                    for (Square castlingLoc : board.getCastlingPartnerLocs().get(movingColor)) {
                        if (isCastlingPseudolegal(board, movingColor, castlingLoc)) {

                            pseudolegalMovesFromSquare.add(this.movePool.createMove(
                                    from, castlingLoc, movingPiece, movingPieceType, movingPieceWasUnmoved,
                                    null, null, false, true, null,
                                    null, null, movementRule));
                        }
                    }
                }
                continue;
            }

            Square newEpMoveTarget = null;
            Square newEpCaptureTarget = null;

            for (int steps = 1; steps <= movementRule.maxSteps(); steps++) {
                int toX = from.x() + steps * movementRule.dx();
                int toY = from.y() + steps * movementRule.dy() * verticalSign;

                if (!board.isInBounds(toX, toY)) {
                    break;
                }

                Square to = board.getSquare(toX, toY);
                if (movementRule.hasModifier(MovementModifier.ENABLES_EN_PASSANT)) {
                    if (steps == 1) {
                        newEpMoveTarget = to;
                    } else {
                        newEpCaptureTarget = to;
                    }
                }

                Set<PieceType> promoPieceTypes;
                if (movingPiece.type().role().equals(PieceRole.PAWN_LIKE)
                        && board.getBackRankIndex(movingColor.getOpponent()) == toY) {
                    promoPieceTypes = board.getPromoOptions();
                } else {
                    promoPieceTypes = new HashSet<>();
                    promoPieceTypes.add(null);
                }

                Piece toPiece = board.getPieceAt(to);

                if (toPiece == null) {
                    if (steps >= movementRule.minSteps()) {

                        if (!movementRule.hasModifier(MovementModifier.ONLY_CAPTURES) || includeAttacksOnEmptySquares) {
                            for (PieceType promoPieceType : promoPieceTypes) {

                                pseudolegalMovesFromSquare.add(this.movePool.createMove(
                                        from, to, movingPiece, movingPieceType, movingPieceWasUnmoved,
                                        null, null, false, false, newEpMoveTarget,
                                        newEpCaptureTarget, promoPieceType, movementRule));
                            }

                        } else if (to.equals(board.getEnPassantMoveTarget()) &&
                                movementRule.hasModifier(MovementModifier.CAPTURES_EN_PASSANT)) {
                            for (PieceType promoPieceType : promoPieceTypes) {

                                pseudolegalMovesFromSquare.add(this.movePool.createMove(
                                        from, to, movingPiece, movingPieceType, movingPieceWasUnmoved,
                                        board.getPieceAt(prevEpCaptureTarget), prevEpCaptureTarget, false, false, null,
                                        null, promoPieceType, movementRule));
                            }
                        }
                    }

                } else if (toPiece.color().equals(movingColor)) {
                    break;

                } else {
                    if ((steps >= movementRule.minSteps()) &&
                            !movementRule.hasModifier(MovementModifier.ONLY_NON_CAPTURES)) {
                        for (PieceType promoPieceType : promoPieceTypes) {

                            pseudolegalMovesFromSquare.add(this.movePool.createMove(
                                    from, to, movingPiece, movingPieceType, movingPieceWasUnmoved,
                                    toPiece, to, board.isUnmovedAt(to), false, null,
                                    null, promoPieceType, movementRule));

                            // TODO: Find a nicer way to solve the stopOnFirstCheck issue
                            if (stopOnFirstCheck && to.equals(board.getKingLocs().get(movingColor.getOpponent()))) {

                                return pseudolegalMovesFromSquare;
                            }
                        }
                    }
                    break;
                }
            }
        }
        return pseudolegalMovesFromSquare;
    }

    private boolean isKingInCheck(Board board, Color kingColor) {
        List<Move> pseudolegalMoves = calculatePseudolegalMoves(board, kingColor.getOpponent(), true, false);
        Square kingSquare = board.getKingLocs().get(kingColor);
        boolean result = pseudolegalMoves.stream()
                .anyMatch(move -> move.getTo().equals(kingSquare)
                        && move.getMovingPiece().color().equals(kingColor.getOpponent())
                        && move.isCapture());
        this.movePool.releaseAllMoves(pseudolegalMoves);
        return result;
    }

    // TODO: Doesn't work for empty squares because ONLY_CAPTURES moves (specifically pawn attacks) are then not
    //  captured by calculatePseudolegalMoves => DONE, but still needs testing
    private boolean isSquareAttacked(Board board, Square square, Color attackingColor) {
        List<Move> pseudolegalMoves = calculatePseudolegalMoves(board, attackingColor, false, true);
        // TODO: Find a nicer solution here (a complex check via MovementModifier.ONLY_NON_CAPTURES is required, as
        //  move.isCapture() would return false for empty squares)
        boolean result = pseudolegalMoves.stream()
                .anyMatch(move -> move.getTo().equals(square)
                        && !move.getMovementRule().hasModifier(MovementModifier.ONLY_NON_CAPTURES));
        this.movePool.releaseAllMoves(pseudolegalMoves);
        return result;
    }

    private boolean isCastlingPseudolegal(Board board, Color colorToMove, Square castlingPartnerFrom) {
        Square kingFrom = board.getKingLocs().get(colorToMove);
        if (kingFrom.y() != castlingPartnerFrom.y() || !board.isUnmovedAt(castlingPartnerFrom)) {
            return false;
        }

        int kingToX;
        int castlingPartnerToX;
        if (kingFrom.x() > castlingPartnerFrom.x()) {
            // Long castling
            kingToX = 2;
            castlingPartnerToX = 3;
        } else {
            // Short casting
            kingToX = board.getWidth() - 2;
            castlingPartnerToX = board.getWidth() - 3;
        }

        int totalMinX = Math.min(Math.min(kingFrom.x(), kingToX), Math.min(castlingPartnerFrom.x(),
                castlingPartnerToX));
        int totalMaxX = Math.max(Math.max(kingFrom.x(), kingToX), Math.max(castlingPartnerFrom.x(),
                castlingPartnerToX));

        int y = kingFrom.y();

        for (int x = totalMinX; x <= totalMaxX; x++) {
            if ((x != kingFrom.x()) && (x != castlingPartnerFrom.x()) && (board.getPieceAt(x, y) != null)) {
                return false;
            }
        }
        return true;
    }

    private boolean isCastlingLegal(Board board, Move castlingMove) {
        int kingFromX = castlingMove.getFrom().x();
        int kingToX = (kingFromX > castlingMove.getTo().x()) ? 2 : (board.getWidth() - 2);

        int kingMinX = Math.min(kingFromX, kingToX);
        int kingMaxX = Math.max(kingFromX, kingToX);

        int y = castlingMove.getFrom().y();

        for (int x = kingMinX; x <= kingMaxX; x++) {
            if (isSquareAttacked(board, board.getSquare(x, y), castlingMove.getMovingPiece().color().getOpponent())) {
                return false;
            }
        }
        return true;
    }
}
