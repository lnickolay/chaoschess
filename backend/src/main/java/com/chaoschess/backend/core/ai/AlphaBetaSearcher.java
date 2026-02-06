package com.chaoschess.backend.core.ai;

import com.chaoschess.backend.core.engine.board.Board;
import com.chaoschess.backend.core.engine.Move;
import com.chaoschess.backend.core.engine.RuleProcessor;
import com.chaoschess.backend.core.model.GameOutcome;
import com.chaoschess.backend.core.model.GameOutcomeCategory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AlphaBetaSearcher {

    public static long alphaBetaCallCounter = 0;

    private final RuleProcessor ruleProcessor;
    private final BoardEvaluator boardEvaluator;

    private long boardsEvaluatedPerMove;
    private long leafNodesEvaluatedPerMove;

    public AlphaBetaSearcher(RuleProcessor ruleProcessor, BoardEvaluator boardEvaluator) {
        this.ruleProcessor = ruleProcessor;
        this.boardEvaluator = boardEvaluator;
    }

    public long getBoardsEvaluatedPerMove() {
        return boardsEvaluatedPerMove;
    }

    public long getLeafNodesEvaluatedPerMove() {
        return leafNodesEvaluatedPerMove;
    }

    public Move findBestMove(Board board, int depth) throws InterruptedException {
        this.boardsEvaluatedPerMove = 0;
        int bestScore = Integer.MIN_VALUE;
        Move bestMove = null;

        List<Move> legalMoves = ruleProcessor.calculateLegalMoves(board);

        orderMoves(legalMoves);

        for (Move move : legalMoves) {
            board.makeMove(move);
            // Call the recursive function (with negated window)
            // Integer.MIN_VALUE must be incremented by 1 because -Integer.MIN_VALUE == Integer.MIN_VALUE, due to the
            // smallest int value in Java having no positive complement
            int score = -alphaBeta(board, depth - 1, Integer.MIN_VALUE + 1, Integer.MAX_VALUE, 1, true);
            board.unmakeMove(move);

            if (score > bestScore) {
                if (bestMove != null) {
                    this.ruleProcessor.getMovePool().releaseMove(bestMove);
                }
                bestScore = score;
                bestMove = move;
            } else {
                this.ruleProcessor.getMovePool().releaseMove(move);
            }
        }
        return bestMove;
    }

    private int alphaBeta(Board board, int depth, int alpha, int beta, int ply, boolean useQuiescenceSearch)
            throws InterruptedException {
        alphaBetaCallCounter++;

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("AI search canceled by user action.");
        }

        List<Move> legalMoves = ruleProcessor.calculateLegalMoves(board);

        GameOutcome gameOutcome = ruleProcessor.determineGameOutcome(board, legalMoves);

        if (((depth == 0) && !useQuiescenceSearch) || (gameOutcome.getCategory() != GameOutcomeCategory.ONGOING)) {
            this.ruleProcessor.getMovePool().releaseAllMoves(legalMoves);
            this.boardsEvaluatedPerMove++;
            this.leafNodesEvaluatedPerMove++;
            return boardEvaluator.evaluate(board, ply, gameOutcome);
        } else if (depth == 0) {
            this.ruleProcessor.getMovePool().releaseAllMoves(legalMoves);
            // Do NOT swap and negate alpha and beta here, as the quiescence search continues on the current node first
            // (rather than on a child node)
            return quiescenceSearch(board, alpha, beta, ply);
        }

        orderMoves(legalMoves);

        int moveIndex = 0;

        for (Move move : legalMoves) {

            board.makeMove(move);
            // Recursive call (with negated window)
            int score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1, useQuiescenceSearch);
            board.unmakeMove(move);

            // Alpha-beta pruning logic
            // TODO: Find out if this ALSO needs to be >= instead of > here, like in quiescence search
            if (score >= beta) {
                this.ruleProcessor.getMovePool().releaseAllMoves(legalMoves.subList(moveIndex, legalMoves.size()));
                // TODO: Figure out what difference it makes here whether to return beta or standPat (keywords:
                //  "fail-soft" vs "fail-hard")
                return beta;
            }
            alpha = Math.max(alpha, score);

            this.ruleProcessor.getMovePool().releaseMove(move);
            moveIndex++;
        }
        return alpha;
    }

    private int quiescenceSearch(Board board, int alpha, int beta, int ply) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("AI search canceled by user action.");
        }

        // TODO: Redundant calculation on initial call (already handled at depth == 0 in alphaBeta())
        List<Move> legalMoves = ruleProcessor.calculateLegalMoves(board);
        // TODO: Unnecessary on initial call
        GameOutcome gameOutcome = ruleProcessor.determineGameOutcome(board, legalMoves);

        if (gameOutcome.getCategory() != GameOutcomeCategory.ONGOING) {
            this.ruleProcessor.getMovePool().releaseAllMoves(legalMoves);
            this.boardsEvaluatedPerMove++;
            this.leafNodesEvaluatedPerMove++;
            return boardEvaluator.evaluate(board, ply, gameOutcome);
        }

        this.boardsEvaluatedPerMove++;
        int standPat = boardEvaluator.evaluate(board, ply, gameOutcome);

        // TODO: Figure out what difference it makes here whether to return beta or standPat
        if (standPat >= beta) {
            this.ruleProcessor.getMovePool().releaseAllMoves(legalMoves);
            return beta;
        }

        if (standPat > alpha) {
            alpha = standPat;
        }

        // Optional hard cutoff to prevent tactical sequences from going too deep
        final int MAX_QS_PLY = 12;
        if (ply >= MAX_QS_PLY) {
            this.ruleProcessor.getMovePool().releaseAllMoves(legalMoves);
            this.leafNodesEvaluatedPerMove++;
            return alpha;
        }

        List<Move> tacticalMoves = new ArrayList<>();
        for (Move move : legalMoves) {
            if (move.isCapture() || move.isPromo()) {
                tacticalMoves.add(move);
            } else {
                this.ruleProcessor.getMovePool().releaseMove(move);
            }
        }

        if (tacticalMoves.isEmpty()) {
            this.leafNodesEvaluatedPerMove++;
            return alpha;
        }

        orderMoves(tacticalMoves);

        int moveIndex = 0;

        for (Move move : tacticalMoves) {
            board.makeMove(move);
            // Recursive call (with negated window)
            int score = -quiescenceSearch(board, -beta, -alpha, ply + 1);
            board.unmakeMove(move);

            // Alpha-beta pruning logic
            // TODO: Find out why this has to be >= instead of >, so that the quiescence search works properly
            if (score >= beta) {
                this.ruleProcessor.getMovePool().releaseAllMoves(tacticalMoves.subList(moveIndex,
                        tacticalMoves.size()));
                // TODO: Figure out what difference it makes here whether to return beta or score
                return beta;
            }
            alpha = Math.max(alpha, score);

            this.ruleProcessor.getMovePool().releaseMove(move);
            moveIndex++;
        }

        return alpha;
    }

    private void orderMoves(List<Move> moves) {
        moves.sort(Comparator.comparingInt(this::scoreMoveForOrdering).reversed());
    }

    private int scoreMoveForOrdering(Move move) {
        int score = 0;

        if (move.isCapture()) {
            int movingPieceValue = BoardEvaluator.MATERIAL_VALUES.get(move.getMovingPiece().type().name());
            int capturedPieceValue = BoardEvaluator.MATERIAL_VALUES.get(move.getCapturedPiece().type().name());
            // TODO: Potentially suboptimal: value could become negative with fairy chess pieces (e.g., amazon capturing
            //  a pawn)
            score += 10 * capturedPieceValue - movingPieceValue;
        }

        if (move.isPromo()) {
            int promoPieceValue = BoardEvaluator.MATERIAL_VALUES.get(move.getPromoPieceType().name());
            // TODO: Weighted too lightly compared to captures
            score += promoPieceValue;
        }

        return score;
    }
}
