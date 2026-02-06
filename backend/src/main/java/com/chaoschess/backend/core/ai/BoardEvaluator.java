package com.chaoschess.backend.core.ai;

import com.chaoschess.backend.core.engine.board.Board;
import com.chaoschess.backend.core.model.Color;
import com.chaoschess.backend.core.model.GameOutcome;
import com.chaoschess.backend.core.model.GameOutcomeCategory;
import com.chaoschess.backend.core.model.PieceRole;
import com.chaoschess.backend.core.model.PieceType;
import com.chaoschess.backend.core.model.PieceTypes;
import com.chaoschess.backend.core.model.Square;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class BoardEvaluator {

    // TODO: Why String keys and not PieceType keys?
    // TODO: Consider putting the material values directly in piece_types.json instead
    // TODO: Consider modifying these values based on board size
    public static final Map<String, Integer> MATERIAL_VALUES = Map.ofEntries(
            Map.entry("King", 0),
            Map.entry("Queen", 900),
            Map.entry("Rook", 500),
            Map.entry("Bishop", 300),
            Map.entry("Knight", 300),
            Map.entry("Pawn", 100),
            // Fairy chess pieces
            Map.entry("Wazir", 120),
            Map.entry("Ferz", 100),
            Map.entry("Alfil", 80),
            Map.entry("Zebra", 280),
            Map.entry("Giraffe", 230),
            Map.entry("Mann", 300),
            Map.entry("Archbishop", 780),
            Map.entry("Chancellor", 850),
            Map.entry("Amazon", 1250),
            Map.entry("Centaur", 550),
            Map.entry("Nightrider", 650)
    );

    private static final int MATE_SCORE = 1000000;
    private static final int MIDGAME_MATERIAL_CUTOFF = 3200;
    private static final int ENDGAME_MATERIAL_CUTOFF = 1400;

    // TODO: Never accessed at the moment
    private final PieceTypes pieceTypes;

    private Map<PieceType, int[][]> midgamePSTs;
    private Map<PieceType, int[][]> endgamePSTs;

    public BoardEvaluator(PieceTypes pieceTypes) {
        this.pieceTypes = pieceTypes;
    }

    public void initializePSTs(int width, int height, PieceTypes pieceTypes) {
        PSTData pstData = PSTGenerator.generatePSTs(width, height, pieceTypes);
        this.midgamePSTs = pstData.midgamePSTs();
        this.endgamePSTs = pstData.endgamePSTs();
    }

    public int evaluate(Board board, int ply, GameOutcome gameOutcome) {
        Color hero = board.getColorToMove();
        Color villain = hero.getOpponent();

        if (gameOutcome.getCategory() == GameOutcomeCategory.DRAW) {
            return 0;
        } else if (gameOutcome.getCategory() == GameOutcomeCategory.WIN_LOSS) {
            int sign = gameOutcome.winner().equals(hero) ? 1 : -1;
            // Subtract ply here to prioritize faster mates
            return sign * (MATE_SCORE - ply);
        }

        MaterialValues materialValues = countMaterialValues(board);

        int totalMaterialValueExcludingPawns = materialValues.excludingPawns().get(hero)
                + materialValues.excludingPawns().get(villain);
        double pstMidgameWeightingFactor = ((double) totalMaterialValueExcludingPawns - ENDGAME_MATERIAL_CUTOFF)
                / (MIDGAME_MATERIAL_CUTOFF - ENDGAME_MATERIAL_CUTOFF);
        pstMidgameWeightingFactor = Math.clamp(pstMidgameWeightingFactor, 0.0, 1.0);

        PositionalValues positionalValues = countPositionalValues(board, pstMidgameWeightingFactor);

        int materialScore = materialValues.total().get(hero) - materialValues.total().get(villain);
        int positionalScore = positionalValues.total().get(hero) - positionalValues.total().get(villain);

        return materialScore + positionalScore;
    }

    private MaterialValues countMaterialValues(Board board) {
        Map<Color, Integer> totalValues = new EnumMap<>(Color.class);
        Map<Color, Integer> excludingPawnsValues = new EnumMap<>(Color.class);

        for (Color color : Color.values()) {
            int onlyPawnsValue = 0;
            int excludingPawnsValue = 0;

            for (Square pieceLoc : board.getPieceLocs().get(color)) {
                PieceType pieceType = board.getPieceAt(pieceLoc).type();
                if (pieceType.role() == PieceRole.PAWN_LIKE) {
                    onlyPawnsValue += MATERIAL_VALUES.get(pieceType.name());
                } else {
                    excludingPawnsValue += MATERIAL_VALUES.get(pieceType.name());
                }
            }
            totalValues.put(color, onlyPawnsValue + excludingPawnsValue);
            excludingPawnsValues.put(color, excludingPawnsValue);
        }
        return new MaterialValues(totalValues, excludingPawnsValues);
    }

    private PositionalValues countPositionalValues(Board board, double pstMidgameWeightingFactor) {
        Map<Color, Integer> positionalValues = new EnumMap<>(Color.class);

        for (Color color : Color.values()) {
            double totalValue = 0;

            for (Square pieceLoc : board.getPieceLocs().get(color)) {
                PieceType pieceType = board.getPieceAt(pieceLoc).type();
                int pstX = pieceLoc.x();
                int pstY = color.equals(Color.WHITE) ? pieceLoc.y() : ((board.getHeight() - 1) - pieceLoc.y());
                totalValue += pstMidgameWeightingFactor * this.midgamePSTs.get(pieceType)[pstX][pstY]
                        + (1.0 - pstMidgameWeightingFactor) * this.endgamePSTs.get(pieceType)[pstX][pstY];
            }
            positionalValues.put(color, (int) Math.round(totalValue));
        }
        return new PositionalValues(positionalValues);
    }
}
