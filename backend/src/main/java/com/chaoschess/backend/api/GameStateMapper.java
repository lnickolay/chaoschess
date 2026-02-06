package com.chaoschess.backend.api;

import com.chaoschess.backend.core.engine.Move;
import com.chaoschess.backend.core.model.Color;
import com.chaoschess.backend.core.model.ImmutableBoard;
import com.chaoschess.backend.core.model.GameOutcome;
import com.chaoschess.backend.core.model.Piece;
import com.chaoschess.backend.core.model.PieceType;
import com.chaoschess.backend.core.utils.BoardUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GameStateMapper {

    private record MoveKey(int fromSquareIndex, int toSquareIndex) {}

    public GameStateDTO toDTO(ImmutableBoard immutableBoard, List<Move> pseudolegalMoves, List<Move> legalMoves,
                              GameOutcome gameOutcome, Set<Color> botColors) {
        int width = immutableBoard.width();
        int height = immutableBoard.height();

        GameStateDTO.PieceData[] grid = new GameStateDTO.PieceData[width * height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int squareIndexTo = BoardUtils.coordsToSquareIndex(x, y, width, height);
                Piece piece = immutableBoard.getPieceAt(x, y);
                if (piece != null) {
                    grid[squareIndexTo] = new GameStateDTO.PieceData(piece.type().name(), piece.color());
                } else {
                    grid[squareIndexTo] = null;
                }
            }
        }

        List<String> promoOptionNames = immutableBoard.promoOptions().stream()
                .map(PieceType::name)
                .toList();

        Set<MoveKey> legalMoveKeys = legalMoves.stream()
                .map(legalMove -> new MoveKey(
                        BoardUtils.squareToSquareIndex(legalMove.getFrom(), width, height),
                        BoardUtils.squareToSquareIndex(legalMove.getTo(), width, height)))
                .collect(Collectors.toSet());
        List<GameStateDTO.MoveOption> pseudolegalMoveOptions = new ArrayList<>(pseudolegalMoves.size());

        for (Move pseudolegalMove : pseudolegalMoves) {
            int fromSquareIndex = BoardUtils.squareToSquareIndex(pseudolegalMove.getFrom(), width, height);
            int toSquareIndex = BoardUtils.squareToSquareIndex(pseudolegalMove.getTo(), width, height);
            boolean isPromo = pseudolegalMove.isPromo();
            boolean isLegal = legalMoveKeys.contains(new MoveKey(fromSquareIndex, toSquareIndex));
            pseudolegalMoveOptions.add(new GameStateDTO.MoveOption(fromSquareIndex, toSquareIndex, isPromo, isLegal));
        }

        return new GameStateDTO(width, height, immutableBoard.colorToMove(), grid, promoOptionNames,
                pseudolegalMoveOptions, gameOutcome, botColors);
    }
}
