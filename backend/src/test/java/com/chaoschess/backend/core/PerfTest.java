package com.chaoschess.backend.core;

import com.chaoschess.backend.core.engine.board.Board;
import com.chaoschess.backend.core.engine.MovePool;
import com.chaoschess.backend.core.engine.RuleProcessor;
import com.chaoschess.backend.core.engine.board.BoardFactory;
import com.chaoschess.backend.core.model.PieceTypes;
import com.chaoschess.backend.core.service.ConfigLoader;
import com.chaoschess.backend.core.utils.ZobristKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PerfTest {

    private PieceTypes pieceTypes;
    private PerfTestTool perfTestTool;

    @BeforeEach
    public void setUp() {
        ConfigLoader configLoader = new ConfigLoader();
        this.pieceTypes = new PieceTypes(configLoader.loadPieceTypes());
        ZobristKeys.initializeKeys(16, 16, this.pieceTypes.pieceTypesMap().size());

        MovePool movePool = new MovePool();
        RuleProcessor ruleProcessor = new RuleProcessor(movePool);
        this.perfTestTool = new PerfTestTool(ruleProcessor);
    }

    @ParameterizedTest(name = "Position {0} at depth {1} should have {2} nodes")
    @DisplayName("Move generation validation using standard perft positions")
    @CsvFileSource(resources = "/perft_results.csv")
    public void testPerftResults(String fen, int depth, long expectedResult) {
        Board board = BoardFactory.createBoardFromFen(fen, this.pieceTypes);

        long actualResult = this.perfTestTool.calculatePerft(board, depth);

        assertEquals(expectedResult, actualResult,
                () -> String.format("Perft result for FEN %s at depth %d is incorrect. Expected: %,d. Found: %,d.",
                        fen, depth, expectedResult, actualResult));
    }
}
