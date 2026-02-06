package com.chaoschess.backend.core.utils;

import com.chaoschess.backend.api.MoveRequestDTO;
import com.chaoschess.backend.core.engine.board.Board;
import com.chaoschess.backend.core.engine.Move;
import com.chaoschess.backend.core.model.Color;
import com.chaoschess.backend.core.model.PieceType;
import com.chaoschess.backend.core.model.PieceTypes;
import com.chaoschess.backend.core.model.Piece;
import com.chaoschess.backend.core.model.Square;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BoardUtils {

    private BoardUtils() {}

    private static final Pattern MOVE_NOTATION_PATTERN = Pattern.compile("([a-z]+\\d+)([a-z]+\\d+)([A-Z]|)");

    // This method is handy for debugging via console output
    public static void printBoard(Board board) {
        System.out.println("\n--- BOARD (Move " + board.getFullmoveNumber() + ", " + board.getColorToMove()
                + "'s turn) ---");
        System.out.println("    ---------------");

        for (int y = board.getHeight() - 1; y >= 0; y--) {
            System.out.print((y + 1) + " |");

            for (int x = 0; x < board.getWidth(); x++) {
                Piece piece = board.getPieceAt(x, y);
                String symbol = ".";
                if (piece != null) {
                    symbol = piece.type().symbol();
                    symbol = (piece.color() == Color.WHITE) ? symbol.toUpperCase() : symbol.toLowerCase();
                }
                System.out.print(" " + symbol);
            }
            System.out.println(" |");
        }
        System.out.println("    ---------------");
        System.out.println("    A B C D E F G H");
        System.out.println("-------------------------------------------------");
    }

    public static String squareToNotation(Square square) {
        return String.valueOf((char) ('a' + square.x())) + (square.y() + 1);
    }

    public static Square notationToSquare(String squareNotation) {
        int x = squareNotation.charAt(0) - 'a';
        int y = Integer.parseInt(squareNotation.substring(1)) - 1;
        return new Square(x, y);
    }

    public static String moveToNotation(Move move) {
        String pieceSymbol = move.getMovingPiece().type().symbol();
        if (pieceSymbol.equals("P")) {
            pieceSymbol = "";
        }
        String separatorSymbol = move.isCapture() ? "x" : "-";
        String promotionPieceSymbol = "";
        if (move.isPromo()) {
            promotionPieceSymbol = move.getPromoPieceType().symbol();
        }
        return pieceSymbol + squareToNotation(move.getFrom()) + separatorSymbol
                + squareToNotation(move.getTo()) + promotionPieceSymbol;
    }

    // TODO: Is it okay for this method to create objects from the API package?
    public static MoveRequestDTO notationToMoveRequestDTO(String moveNotation, int width, int height,
                                                          PieceTypes pieceTypes) {
        Matcher matcher = MOVE_NOTATION_PATTERN.matcher(moveNotation);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid move string format: " + moveNotation);
        }

        int fromSquareIndex = squareToSquareIndex(notationToSquare(matcher.group(1)), width, height);
        int toSquareIndex = squareToSquareIndex(notationToSquare(matcher.group(2)), width, height);
        PieceType promotionPieceType = pieceTypes.getPieceTypeBySymbol(matcher.group(3));
        String promotionPieceName = (promotionPieceType != null) ? promotionPieceType.name() : null;
        return new MoveRequestDTO(fromSquareIndex, toSquareIndex, promotionPieceName);
    }

    public static int coordsToSquareIndex(int x, int y, int width, int height) {
        return width * (height - 1 - y) + x;
    }

    public static int squareToSquareIndex(Square square, int width, int height) {
        return coordsToSquareIndex(square.x(), square.y(), width, height);
    }

    // TODO: Is it better to pass the board instead of just width and height, so no new Square object has to be created?
    public static Square squareIndexToSquare(int squareIndex, int width, int height) {
        int x = squareIndex % width;
        int y = height - 1 - (squareIndex / width);
        return new Square(x, y);
    }
}
