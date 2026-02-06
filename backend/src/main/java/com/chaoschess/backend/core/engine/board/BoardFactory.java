package com.chaoschess.backend.core.engine.board;

import com.chaoschess.backend.core.model.Color;
import com.chaoschess.backend.core.model.Piece;
import com.chaoschess.backend.core.model.PieceRole;
import com.chaoschess.backend.core.model.PieceType;
import com.chaoschess.backend.core.model.PieceTypes;
import com.chaoschess.backend.core.model.Square;
import com.chaoschess.backend.core.utils.BoardUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BoardFactory {

    private record WeightedOption<T>(double weight, T value) {}

    // TODO: Randomly created boards should be checked for possible mates in two moves and kings that could be in check
    //  in the starting position
    public static Board createRandomInitialBoard(ChaosLevel chaosLevel, long seed, PieceTypes pieceTypes) {
        if (chaosLevel.equals(ChaosLevel.DULL)) {
            return createBoardFromFen(Board.STANDARD_INITIAL_BOARD_FEN, pieceTypes);
        }

        Random rng = new Random(seed);

        // --- DETERMINE BOARD DIMENSIONS AND CREATE BOARD OBJECT ---

        int maxSizeOffset;
        int baseSize;

        switch (chaosLevel) {
            case WEIRD -> {
                maxSizeOffset = 2;
                baseSize = Board.MIN_BOARD_SIZE + maxSizeOffset
                        + rng.nextInt(Board.MAX_BOARD_SIZE - Board.MIN_BOARD_SIZE - maxSizeOffset - 1);
            }
//            case UNHINGED -> { // TODO
//                ...
//            }
            default -> { // Covers ChaosLevel.ODD case
                maxSizeOffset = 1;
                baseSize = 8 + rng.nextInt(2);
            }
        }

        int width = baseSize + rng.nextInt(-maxSizeOffset, maxSizeOffset + 1);
        int height = baseSize + rng.nextInt(-maxSizeOffset, maxSizeOffset + 1);

        Board board = new Board(width, height);

        // --- DETERMINE COUNT AND TYPES OF SETUP ROWS ---

        int setupRowsCount;
        SetupRowType[] setupRowTypes;

        switch (chaosLevel) {
            case WEIRD -> {
                if (height < 8 || rng.nextDouble() < 0.7) {
                    setupRowsCount = 2;
                    var options = List.of(
                            new WeightedOption<>(3.0, new SetupRowType[]{SetupRowType.NON_PAWN_ROW, SetupRowType.PAWN_ROW}),
                            new WeightedOption<>(1.0, new SetupRowType[]{SetupRowType.HALF_EMPTY_NON_PAWN_ROW,
                                    SetupRowType.PAWN_ROW})
                    );
                    setupRowTypes = pickWeighted(options, rng);
                } else {
                    setupRowsCount = 3;
                    var options = List.of(
                            new WeightedOption<>(1.0, new SetupRowType[]{SetupRowType.NON_PAWN_ROW,
                                    SetupRowType.NON_PAWN_ROW, SetupRowType.PAWN_ROW}),
                            new WeightedOption<>(1.0, new SetupRowType[]{SetupRowType.NON_PAWN_ROW,
                                    SetupRowType.HALF_EMPTY_NON_PAWN_ROW, SetupRowType.PAWN_ROW}),
                            new WeightedOption<>(1.0, new SetupRowType[]{SetupRowType.HALF_EMPTY_NON_PAWN_ROW,
                                    SetupRowType.NON_PAWN_ROW, SetupRowType.PAWN_ROW}),
                            new WeightedOption<>(1.0, new SetupRowType[]{SetupRowType.NON_PAWN_ROW, SetupRowType.PAWN_ROW,
                                    SetupRowType.PAWN_ROW}),
                            new WeightedOption<>(1.0, new SetupRowType[]{SetupRowType.NON_PAWN_ROW,
                                    SetupRowType.HALF_EMPTY_PAWN_ROW, SetupRowType.PAWN_ROW}),
                            new WeightedOption<>(1.0, new SetupRowType[]{SetupRowType.NON_PAWN_ROW, SetupRowType.PAWN_ROW,
                                    SetupRowType.HALF_EMPTY_PAWN_ROW}),
                            new WeightedOption<>(1.0, new SetupRowType[]{SetupRowType.NON_PAWN_ROW, SetupRowType.MIXED_ROW,
                                    SetupRowType.PAWN_ROW}),
                            new WeightedOption<>(1.0, new SetupRowType[]{SetupRowType.NON_PAWN_ROW, SetupRowType.EMPTY_ROW,
                                    SetupRowType.PAWN_ROW})
                    );
                    setupRowTypes = pickWeighted(options, rng);
                }
            }
//            case UNHINGED -> { // TODO
//                setupRowsCount = rng.nextInt(2, Math.min((height + 1) / 2, 5));
//                int pawnRowsCount = rng.nextInt(1, setupRowsCount + 1);
//                List<Integer>
//                if (pawnRowsCount > 1) {
//                    ...
//                }
//                startingRowsContents = new List[setupRowsCount];
//                startingRowsContents[setupRowsCount - 1] = List.of(SquareContent.PAWN);
//            }
            default -> { // Covers ChaosLevel.ODD case
                setupRowsCount = 2;
                setupRowTypes = new SetupRowType[]{SetupRowType.NON_PAWN_ROW, SetupRowType.PAWN_ROW};
            }
        }

        // --- DETERMINE SQUARE CONTENTS ---

        SquareContent[][] startingSquaresContents = new SquareContent[width][setupRowsCount];

        double majorPieceChance = 0.5;
        double emptySquareChance = 0.5;

        double minorPieceChance = 1.0 - majorPieceChance;
        double filledSquareChance = 1.0 - emptySquareChance;

        for (int row = 0; row < setupRowsCount; row++) {
            for (int col = 0; col < width; col++) {
                startingSquaresContents[col][row] = switch (setupRowTypes[row]) {
                    case PAWN_ROW -> SquareContent.PAWN;
                    case NON_PAWN_ROW -> pickWeighted(List.of(
                            new WeightedOption<>(majorPieceChance, SquareContent.MAJOR_PIECE),
                            new WeightedOption<>(minorPieceChance, SquareContent.MINOR_PIECE)),
                            rng);
                    case MIXED_ROW -> pickWeighted(List.of(
                            new WeightedOption<>(1.0, SquareContent.PAWN),
                            new WeightedOption<>(majorPieceChance, SquareContent.MAJOR_PIECE),
                            new WeightedOption<>(minorPieceChance, SquareContent.MINOR_PIECE)),
                            rng);
                    case HALF_EMPTY_PAWN_ROW -> pickWeighted(List.of(
                            new WeightedOption<>(emptySquareChance, SquareContent.EMPTY),
                            new WeightedOption<>(filledSquareChance, SquareContent.PAWN)),
                            rng);
                    case HALF_EMPTY_NON_PAWN_ROW -> pickWeighted(List.of(
                            new WeightedOption<>(emptySquareChance, SquareContent.EMPTY),
                            new WeightedOption<>(filledSquareChance * majorPieceChance, SquareContent.MAJOR_PIECE),
                            new WeightedOption<>(filledSquareChance * minorPieceChance, SquareContent.MINOR_PIECE)),
                            rng);
                    case EMPTY_ROW -> SquareContent.EMPTY;
                };
            }
        }

        // Put the king into a random square on the base row
        int kingCol = rng.nextInt(width);
        startingSquaresContents[kingCol][0] = SquareContent.KING;

        // --- DETERMINE ACTUAL PIECE TYPES AND PLACE THE PIECES ON THE BOARD ---

        for (int row = 0; row < setupRowsCount; row++) {
            for (int col = 0; col < width; col++) {
                SquareContent squareContent = startingSquaresContents[col][row];
                String pieceName = null;

                // Determine the actual piece type
                switch (chaosLevel) {
                    case WEIRD -> pieceName = switch (squareContent) {
                        case KING -> "King";
                        // TODO: Major vs minor piece categorization could be derived from material values
                        case MAJOR_PIECE -> pickWeighted(List.of(
                                new WeightedOption<>(1.0, "Queen"),
                                new WeightedOption<>(1.0, "Rook"),
                                new WeightedOption<>(1.0, "Archbishop"),
                                new WeightedOption<>(1.0, "Chancellor"),
                                new WeightedOption<>(1.0, "Amazon"),
                                new WeightedOption<>(1.0, "Centaur"),
                                new WeightedOption<>(1.0, "Nightrider")
                                ), rng);
                        case MINOR_PIECE -> pickWeighted(List.of(
                                new WeightedOption<>(1.0, "Bishop"),
                                new WeightedOption<>(1.0, "Knight"),
                                new WeightedOption<>(1.0, "Wazir"),
                                new WeightedOption<>(1.0, "Ferz"),
                                new WeightedOption<>(1.0, "Alfil"),
                                new WeightedOption<>(1.0, "Zebra"),
                                new WeightedOption<>(1.0, "Giraffe"),
                                new WeightedOption<>(1.0, "Mann")
                                ), rng);
                        case PAWN -> "Pawn";
                        case EMPTY -> null;
                    };
//                    case UNHINGED -> { // TODO
//                        ...
//                    }
                    default -> pieceName = switch (squareContent) { // Covers ChaosLevel.ODD case
                        case KING -> "King";
                        // TODO: Major vs minor piece categorization could be derived from material values
                        case MAJOR_PIECE -> pickWeighted(List.of(
                                new WeightedOption<>(3.333, "Queen"),
                                new WeightedOption<>(6.667, "Rook"),
                                new WeightedOption<>(1.0, "Archbishop"),
                                new WeightedOption<>(1.0, "Chancellor"),
                                new WeightedOption<>(1.0, "Amazon"),
                                new WeightedOption<>(1.0, "Centaur"),
                                new WeightedOption<>(1.0, "Nightrider")
                        ), rng);
                        case MINOR_PIECE -> pickWeighted(List.of(
                                new WeightedOption<>(6.0, "Bishop"),
                                new WeightedOption<>(6.0, "Knight"),
                                new WeightedOption<>(1.0, "Wazir"),
                                new WeightedOption<>(1.0, "Ferz"),
                                new WeightedOption<>(1.0, "Alfil"),
                                new WeightedOption<>(1.0, "Zebra"),
                                new WeightedOption<>(1.0, "Giraffe"),
                                new WeightedOption<>(1.0, "Mann")
                        ), rng);
                        case PAWN -> "Pawn";
                        case EMPTY -> null;
                    };
                }

                // Place the determined piece for White and its counterpart on the opposite side for Black
                if (pieceName != null) {
                    for (Color color : Color.values()) {
                        // TODO: Put the logic for adding a piece to the board in its own method
                        int x = col;
                        int y = color.equals(Color.WHITE) ? row : (board.height - 1) - row;

                        Square square = board.getSquare(x, y);
                        PieceType pieceType = pieceTypes.getPieceTypeByName(pieceName);

                        board.pieceGrid[x][y] = new Piece(pieceType, color);
                        board.isUnmovedGrid[x][y] = true;
                        board.pieceLocs.get(color).add(square);
                        if (pieceType.role().equals(PieceRole.KING_LIKE)) {
                            board.kingLocs.put(color, square);
                        }
                        if (pieceType.role().equals(PieceRole.ROOK_LIKE)) {
                            board.castlingPartnerLocs.get(color).add(square);
                        }
                    }
                }
            }
        }

        return board;
    }

    // TODO: Is passing pieceTypes necessary? Other solutions?
    // TODO: Maybe set board attributes via methods instead of direct access?
    public static Board createBoardFromFen(String fen, PieceTypes pieceTypes) {
        int width = 8;
        int height = 8;
        Board board = new Board(width, height);

        // Split FEN string into its six components
        String[] fenParts = fen.split(" ");
        if (fenParts.length != 6) {
            throw new IllegalArgumentException("Invalid FEN. Expected 6 parts, found " + fenParts.length + ".");
        }

        // Parse piece placement fromSquareIndex FEN string
        String[] fenRanks = fenParts[0].split("/");
        if (fenRanks.length != board.height) {
            throw new IllegalArgumentException("Invalid FEN. Expected " + board.height + " ranks, found "
                    + fenRanks.length + ".");
        }
        for (int fenRankIndex = 0; fenRankIndex < board.height; fenRankIndex++) {
            int x = 0;
            int y = (board.height - 1) - fenRankIndex;
            for (char c : fenRanks[fenRankIndex].toCharArray()) {
                if (Character.isDigit(c)) {
                    x += Character.getNumericValue(c);
                } else {
                    Square square = board.getSquare(x, y);
                    PieceType pieceType = pieceTypes.getPieceTypeBySymbol(String.valueOf(c).toUpperCase());
                    if (pieceType == null) {
                        throw new IllegalArgumentException("Invalid FEN. Contains unknown piece symbol '" + c + "'.");
                    }
                    Color color = Character.isUpperCase(c) ? Color.WHITE : Color.BLACK;

                    // hasMoved gets set to false only for pawns not in their starting row, true for every other piece
                    // hasMoved might still be set to false for kings and rooks later, depending on the castling
                    // availability specified in the FEN string
                    // TODO: possibly compare the positions of all pieces with their potential standard starting
                    //  positions and set hasMoved accordingly (works as is, though)
                    boolean hasMoved = !(pieceType.name().equals("Pawn")
                            && ((color.equals(Color.WHITE) && (y == 1))
                            || (color.equals(Color.BLACK) && (y == (board.height - 2)))));

                    board.pieceGrid[x][y] = new Piece(pieceType, color);
                    board.isUnmovedGrid[x][y] = !hasMoved;
                    board.pieceLocs.get(color).add(square);
                    if (pieceType.role().equals(PieceRole.KING_LIKE)) {
                        board.kingLocs.put(color, square);
                    }
                    if (pieceType.role().equals(PieceRole.ROOK_LIKE)) {
                        board.castlingPartnerLocs.get(color).add(square);
                    }
                    x++;
                }
            }
            if (x != board.width) {
                throw new IllegalArgumentException("Invalid FEN. Rank " + (y + 1) + " has " + x + "squares (should be "
                        + board.width + ").");
            }
        }

        // Parse active color fromSquareIndex FEN string
        board.colorToMove = fenParts[1].equals("w") ? Color.WHITE : Color.BLACK;

        // Parse castling availability fromSquareIndex FEN string
        List<Square> rookLocs = new ArrayList<>();
        Square whiteKingLoc = board.getKingLocs().get(Color.WHITE);     // TODO: weniger hässlich lösen
        Square blackKingLoc = board.getKingLocs().get(Color.BLACK);
        if (fenParts[2].contains("Q")) {
            board.isUnmovedGrid[whiteKingLoc.x()][whiteKingLoc.y()] = true;
            rookLocs.add(board.getSquare(0, 0));
        }
        if (fenParts[2].contains("K")) {
            board.isUnmovedGrid[whiteKingLoc.x()][whiteKingLoc.y()] = true;
            rookLocs.add(board.getSquare(board.width - 1, 0));
        }
        if (fenParts[2].contains("q")) {
            board.isUnmovedGrid[blackKingLoc.x()][blackKingLoc.y()] = true;
            rookLocs.add(board.getSquare(0, board.height - 1));
        }
        if (fenParts[2].contains("k")) {
            board.isUnmovedGrid[blackKingLoc.x()][blackKingLoc.y()] = true;
            rookLocs.add(board.getSquare(board.width - 1, board.height - 1));
        }
        for (Square rookLoc : rookLocs) {
            board.isUnmovedGrid[rookLoc.x()][rookLoc.y()] = true;
        }

        // Parse en passant target square fromSquareIndex FEN string
        if (!fenParts[3].equals("-")) {
            board.enPassantMoveTarget = BoardUtils.notationToSquare(fenParts[3]);
            int enPassantCaptureTargetY = board.colorToMove.equals(Color.WHITE)
                    ? (board.enPassantMoveTarget.y() - 1) : (board.enPassantMoveTarget.y() + 1);
            board.enPassantCaptureTarget = board.getSquare(
                    board.enPassantMoveTarget.x(), enPassantCaptureTargetY);
        }

        // Parse halfmove clock fromSquareIndex FEN string
        board.halfmoveClock = Integer.parseInt(fenParts[4]);

        // Setup fullmove number variable and parse fullmove number fromSquareIndex FEN string
        board.fullmoveNumber = Integer.parseInt(fenParts[5]);

        board.promoOptions.add(pieceTypes.getPieceTypeByName("Queen"));
        board.promoOptions.add(pieceTypes.getPieceTypeByName("Rook"));
        board.promoOptions.add(pieceTypes.getPieceTypeByName("Bishop"));
        board.promoOptions.add(pieceTypes.getPieceTypeByName("Knight"));

        board.zobristHash = board.calculateFullZobristHash();

        return board;
    }

    private static <T> T pickWeighted(List<WeightedOption<T>> options, Random rng) {
        double totalWeight = 0.0;
        for (WeightedOption<T> option : options) {
            totalWeight += option.weight;
        }

        double threshold = rng.nextDouble() * totalWeight;
        double cumulative = 0.0;
        for (WeightedOption<T> option : options) {
            cumulative += option.weight;
            if (threshold < cumulative) {
                return option.value();
            }
        }
        return options.getLast().value();
    }
}
