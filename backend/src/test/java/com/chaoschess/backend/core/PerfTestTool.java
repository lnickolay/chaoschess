package com.chaoschess.backend.core;

import com.chaoschess.backend.core.engine.board.Board;
import com.chaoschess.backend.core.engine.Move;
import com.chaoschess.backend.core.engine.RuleProcessor;

import java.util.List;

public class PerfTestTool {

    private final RuleProcessor ruleProcessor;

    public PerfTestTool(RuleProcessor ruleProcessor) {
        this.ruleProcessor = ruleProcessor;
    }

    public long calculatePerft(Board board, int depth) {
        if (depth == 0) {
            return 1;
        }
        List<Move> legalMoves = this.ruleProcessor.calculateLegalMoves(board);
        long possibleMoveSequencesCount = 0;

        for (Move legalMove : legalMoves) {
            board.makeMove(legalMove);
            possibleMoveSequencesCount += calculatePerft(board, depth - 1);
            board.unmakeMove(legalMove);

            this.ruleProcessor.getMovePool().releaseMove(legalMove);
        }
        return possibleMoveSequencesCount;
    }
}
