package com.chaoschess.backend.core.engine.board;

import com.chaoschess.backend.core.engine.Move;
import com.chaoschess.backend.core.model.Color;
import com.chaoschess.backend.core.model.Piece;
import com.chaoschess.backend.core.model.PieceRole;
import com.chaoschess.backend.core.model.PieceType;
import com.chaoschess.backend.core.model.Square;
import com.chaoschess.backend.core.utils.ZobristKeys;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Board {

    public static final int MIN_BOARD_SIZE = 6;
    public static final int MAX_BOARD_SIZE = 12;

    public static final String STANDARD_INITIAL_BOARD_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    final int width;
    final int height;
    final Square[][] squares;
    final Piece[][] pieceGrid;
    final boolean[][] isUnmovedGrid;
    final Map<Color, Set<Square>> pieceLocs;
    final Map<Color, Set<Square>> castlingPartnerLocs;
    final Map<Color, Square> kingLocs;
    final Set<PieceType> promoOptions;
    private final Deque<Square> enPassantMoveTargetHistory;
    private final Deque<Square> enPassantCaptureTargetHistory;
    private final Deque<Integer> halfmoveClockHistory;
    private final List<Long> zobristHashHistory;

    Color colorToMove;
    Square enPassantMoveTarget;
    Square enPassantCaptureTarget;
    int halfmoveClock;
    int fullmoveNumber;
    long zobristHash;

    public int getWidth() { return this.width; }
    public int getHeight() { return this.height; }
    public Map<Color, Set<Square>> getPieceLocs() { return this.pieceLocs; }
    public Map<Color, Set<Square>> getCastlingPartnerLocs() { return this.castlingPartnerLocs; }
    public Map<Color, Square> getKingLocs() { return this.kingLocs; }
    public Set<PieceType> getPromoOptions() { return this.promoOptions; }
    public Color getColorToMove() { return this.colorToMove; }
    public Square getEnPassantMoveTarget() { return this.enPassantMoveTarget; }
    public Square getEnPassantCaptureTarget() { return this.enPassantCaptureTarget; }
    public int getHalfmoveClock() { return this.halfmoveClock; }
    public int getFullmoveNumber() { return this.fullmoveNumber; }
    public long getZobristHash() {return this.zobristHash; }
    public List<Long> getZobristHashHistory() {return this.zobristHashHistory; }

    Board(int width, int height) {
        this.width = width;
        this.height = height;

        this.squares = new Square[this.width][this.height];
        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.height; y++) {
                this.squares[x][y] = new Square(x, y);
            }
        }

        this.pieceGrid = new Piece[this.width][this.height];
        this.isUnmovedGrid = new boolean[this.width][this.height];

        this.pieceLocs = new EnumMap<>(Color.class);
        this.castlingPartnerLocs = new EnumMap<>(Color.class);
        for (Color color : Color.values()) {
            this.pieceLocs.put(color, new HashSet<>());
            this.castlingPartnerLocs.put(color, new HashSet<>());
        }
        this.kingLocs = new EnumMap<>(Color.class);

        this.promoOptions = new HashSet<>();

        // Using LinkedList instead of ArrayDeque as the Deque implementation because the latter does not permit null
        // elements, which are required here
        this.enPassantMoveTargetHistory = new LinkedList<>();
        this.enPassantCaptureTargetHistory = new LinkedList<>();

        this.halfmoveClockHistory = new ArrayDeque<>();
        // Using List instead of Deque because get(i) is required to check for threefold repetition
        this.zobristHashHistory = new ArrayList<>();

        this.colorToMove = Color.WHITE;
        this.enPassantMoveTarget = null;
        this.enPassantCaptureTarget = null;

        this.halfmoveClock = 0;
        this.fullmoveNumber = 1;

        this.zobristHash = 0L;
    }

    public Board deepCopy() {
        return new Board(this);
    }

    private Board(Board other) {
        // Primitives and immutable finals can be assigned directly
        this.width = other.width;
        this.height = other.height;
        this.colorToMove = other.colorToMove;
        this.enPassantMoveTarget = other.enPassantMoveTarget;
        this.enPassantCaptureTarget = other.enPassantCaptureTarget;
        this.halfmoveClock = other.halfmoveClock;
        this.fullmoveNumber = other.fullmoveNumber;

        // Immutable containers with immutable contents can also be assigned directly
        this.squares = other.squares;
        this.promoOptions = other.promoOptions;

        // Shallow copies of the inner arrays are sufficient since their elements are immutable
        this.pieceGrid = new Piece[this.width][];
        this.isUnmovedGrid = new boolean[this.width][];
        for (int x = 0; x < this.width; x++) {
            this.pieceGrid[x] = Arrays.copyOf(other.pieceGrid[x], this.height);
            this.isUnmovedGrid[x] = Arrays.copyOf(other.isUnmovedGrid[x], this.height);
        }

        // Shallow copies of the inner sets of pieceLocs and castlingPartnerLocs and of the kingLocs map are sufficient
        // since their elements are immutable
        this.pieceLocs = new EnumMap<>(Color.class);
        this.castlingPartnerLocs = new EnumMap<>(Color.class);
        for (Color color : Color.values()) {
            this.pieceLocs.put(color, new HashSet<>(other.pieceLocs.get(color)));
            this.castlingPartnerLocs.put(color, new HashSet<>(other.castlingPartnerLocs.get(color)));
        }
        this.kingLocs = new EnumMap<>(other.kingLocs);

        // Shallow copies of the stacks are sufficient since their elements are immutable
        this.enPassantMoveTargetHistory = new LinkedList<>(other.enPassantMoveTargetHistory);
        this.enPassantCaptureTargetHistory = new LinkedList<>(other.enPassantCaptureTargetHistory);
        this.halfmoveClockHistory = new LinkedList<>(other.halfmoveClockHistory);
        this.zobristHashHistory = new ArrayList<>(other.zobristHashHistory);

        this.zobristHash = other.zobristHash;
    }

    long calculateFullZobristHash() {
        long hash = 0;

        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.height; y++) {
                Piece piece = this.pieceGrid[x][y];
                if (piece != null) {
                    hash ^= ZobristKeys.getPieceSquareKey(piece.type().id(), x, y, piece.color().ordinal());
                    if (this.isUnmovedGrid[x][y]) {
                        hash ^= ZobristKeys.getIsUnmovedKey(x, y, piece.color().ordinal());
                    }
                }
            }
        }
        hash ^= ZobristKeys.getColorToMoveKey(this.colorToMove.ordinal());
        if (this.enPassantMoveTarget != null) {
            hash ^= ZobristKeys.getEnPassantMoveTargetKey(this.enPassantMoveTarget.x(), this.enPassantMoveTarget.y());
        }
        if (this.enPassantCaptureTarget != null) {
            hash ^= ZobristKeys.getEnPassantMoveTargetKey(this.enPassantCaptureTarget.x(),
                    this.enPassantCaptureTarget.y());
        }
        return hash;
    }

    // TODO: Possibly remove isInBounds(), incorporate validation here, and return null if out of bounds
    public Square getSquare(int x, int y) {
        return squares[x][y];
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < this.width && y >= 0 && y < this.height;
    }

    public boolean isInBounds(Square square) {
        return isInBounds(square.x(), square.y());
    }

    // TODO: This method exists here and in ImmutableBoard, remove it here or there
    public Piece getPieceAt(int x, int y) {
        return this.pieceGrid[x][y];
    }

    // TODO: This method exists here and in ImmutableBoard, remove it here or there
    public Piece getPieceAt(Square square) {
        return getPieceAt(square.x(), square.y());
    }

    public boolean isUnmovedAt(int x, int y) {
        return this.isUnmovedGrid[x][y];
    }

    public boolean isUnmovedAt(Square square) {
        return isUnmovedAt(square.x(), square.y());
    }

    public int getBackRankIndex(Color color) {
        return color.equals(Color.WHITE) ? 0 : (this.height - 1);
    }

    public boolean isKingUnmoved(Color kingColor) {
        Square kingLoc = this.kingLocs.get(kingColor);
        // TODO: Write a method for isUnmoved query with a Square object as input parameter
        return this.isUnmovedGrid[kingLoc.x()][kingLoc.y()];
    }

    public void makeMove(Move move) {
        Square from = move.getFrom();
        Square to;
        if (!move.getIsCastling()) {
            to = move.getTo();
        } else {
            if (from.x() > move.getTo().x()) {
                // Long castling
                to = getSquare(2, from.y());
            } else {
                // Short castling
                to = getSquare(this.width - 2, from.y());
            }
        }
        Piece movingPiece = move.getMovingPiece();
        Color movingColor = movingPiece.color();

        // --- ALL MOVES ---

        this.zobristHashHistory.add(this.zobristHash);

        this.colorToMove = this.colorToMove.getOpponent();
        this.zobristHash ^= ZobristKeys.getColorToMoveKey(this.colorToMove.ordinal());
        this.zobristHash ^= ZobristKeys.getColorToMoveKey(this.colorToMove.getOpponent().ordinal());

        this.pieceGrid[from.x()][from.y()] = null;
        this.pieceGrid[to.x()][to.y()] = movingPiece;
        this.pieceLocs.get(movingColor).remove(from);
        this.pieceLocs.get(movingColor).add(to);
        if (movingPiece.type().role().equals(PieceRole.ROOK_LIKE)) {
            this.castlingPartnerLocs.get(movingColor).remove(from);
            this.castlingPartnerLocs.get(movingColor).add(to);
        }
        this.zobristHash ^= ZobristKeys.getPieceSquareKey(movingPiece.type().id(), from.x(), from.y(),
                movingColor.ordinal());
        this.zobristHash ^= ZobristKeys.getPieceSquareKey(movingPiece.type().id(), to.x(), to.y(),
                movingColor.ordinal());

        if (move.getMovingPieceWasUnmoved()) {
            // TODO: Add a private setter for this with a Square object as input parameter
            this.isUnmovedGrid[from.x()][from.y()] = false;
            this.zobristHash ^= ZobristKeys.getIsUnmovedKey(from.x(), from.y(), movingColor.ordinal());
        }
        if (movingPiece.type().role().equals(PieceRole.KING_LIKE)) {
            this.kingLocs.put(movingColor, to);
        }

        if (this.enPassantMoveTarget != null) {
            this.zobristHash ^= ZobristKeys.getEnPassantMoveTargetKey(this.enPassantMoveTarget.x(),
                    this.enPassantMoveTarget.y());
        }
        this.enPassantMoveTargetHistory.push(this.enPassantMoveTarget);
        this.enPassantMoveTarget = move.getNewEnPassantMoveTarget();
        if (this.enPassantMoveTarget != null) {
            this.zobristHash ^= ZobristKeys.getEnPassantMoveTargetKey(this.enPassantMoveTarget.x(),
                    this.enPassantMoveTarget.y());
        }

        if (this.enPassantCaptureTarget != null) {
            this.zobristHash ^= ZobristKeys.getEnPassantMoveTargetKey(this.enPassantCaptureTarget.x(),
                    this.enPassantCaptureTarget.y());
        }
        this.enPassantCaptureTargetHistory.push(this.enPassantCaptureTarget);
        this.enPassantCaptureTarget = move.getNewEnPassantCaptureTarget();
        if (this.enPassantCaptureTarget != null) {
            this.zobristHash ^= ZobristKeys.getEnPassantMoveTargetKey(this.enPassantCaptureTarget.x(),
                    this.enPassantCaptureTarget.y());
        }

        this.halfmoveClockHistory.push(this.halfmoveClock);
        if (movingPiece.type().role().equals(PieceRole.PAWN_LIKE) || move.isCapture()) {
            this.halfmoveClock = 0;
        } else {
            this.halfmoveClock++;
        }
        if (this.colorToMove.equals(Color.BLACK)) {
            this.fullmoveNumber++;
        }

        // --- CAPTURE MOVES ONLY ---

        if (move.isCapture()) {
            if (!to.equals(move.getCapturedPieceLoc())) {
                this.pieceGrid[move.getCapturedPieceLoc().x()][move.getCapturedPieceLoc().y()] = null;
            }
            this.pieceLocs.get(move.getCapturedPiece().color()).remove(move.getCapturedPieceLoc());

            if (move.getCapturedPiece().type().role().equals(PieceRole.ROOK_LIKE)) {
                this.castlingPartnerLocs.get(move.getCapturedPiece().color()).remove(move.getCapturedPieceLoc());
            }
            this.zobristHash ^= ZobristKeys.getPieceSquareKey(move.getCapturedPiece().type().id(),
                    move.getCapturedPieceLoc().x(), move.getCapturedPieceLoc().y(),
                    move.getCapturedPiece().color().ordinal());

            if (move.getCapturedPieceWasUnmoved()) {
                this.isUnmovedGrid[move.getCapturedPieceLoc().x()][move.getCapturedPieceLoc().y()] = false;
                this.zobristHash ^= ZobristKeys.getIsUnmovedKey(move.getCapturedPieceLoc().x(),
                        move.getCapturedPieceLoc().y(), move.getCapturedPiece().color().ordinal());
            }
        }

        // --- PROMOTION MOVES ONLY ---

        if (move.isPromo()) {
            this.pieceGrid[to.x()][to.y()] = new Piece(move.getPromoPieceType(), movingColor);
            this.zobristHash ^= ZobristKeys.getPieceSquareKey(movingPiece.type().id(), to.x(), to.y(),
                    movingColor.ordinal());
            this.zobristHash ^= ZobristKeys.getPieceSquareKey(move.getPromoPieceType().id(), to.x(), to.y(),
                    movingColor.ordinal());
        }

        // --- CASTLING MOVES ONLY ---

        if (move.getIsCastling()) {
            Square castlingPartnerFrom = move.getTo();
            Square castlingPartnerTo;
            if (from.x() > castlingPartnerFrom.x()) {
                // Long castling
                castlingPartnerTo = getSquare(3, from.y());
            } else {
                // Short castling
                castlingPartnerTo = getSquare(this.width - 3, from.y());
            }
            Piece castlingPartnerPiece = getPieceAt(castlingPartnerFrom);
            this.pieceGrid[castlingPartnerFrom.x()][castlingPartnerFrom.y()] = null;
            this.pieceGrid[castlingPartnerTo.x()][castlingPartnerTo.y()] = castlingPartnerPiece;
            this.pieceLocs.get(movingColor).remove(castlingPartnerFrom);
            this.pieceLocs.get(movingColor).add(castlingPartnerTo);
            this.castlingPartnerLocs.get(movingColor).remove(castlingPartnerFrom);
            this.castlingPartnerLocs.get(movingColor).add(castlingPartnerTo);
            this.zobristHash ^= ZobristKeys.getPieceSquareKey(castlingPartnerPiece.type().id(), castlingPartnerFrom.x(),
                    castlingPartnerFrom.y(), movingColor.ordinal());
            this.zobristHash ^= ZobristKeys.getPieceSquareKey(castlingPartnerPiece.type().id(), castlingPartnerTo.x(),
                    castlingPartnerTo.y(), movingColor.ordinal());
            this.isUnmovedGrid[castlingPartnerFrom.x()][castlingPartnerFrom.y()] = false;
            this.zobristHash ^= ZobristKeys.getIsUnmovedKey(castlingPartnerFrom.x(),
                    castlingPartnerFrom.y(), movingColor.ordinal());
        }
    }

    public void unmakeMove(Move move) {
        Square from = move.getFrom();
        Square to;
        if (!move.getIsCastling()) {
            to = move.getTo();
        } else {
            if (from.x() > move.getTo().x()) {
                // long castling
                to = getSquare(2, from.y());
            } else {
                // short castling
                to = getSquare(this.width - 2, from.y());
            }
        }
        Piece movingPiece = move.getMovingPiece();
        Color movingColor = movingPiece.color();

        // --- ALL MOVES ---

        this.zobristHashHistory.removeLast();

        this.colorToMove = this.colorToMove.getOpponent();
        this.zobristHash ^= ZobristKeys.getColorToMoveKey(this.colorToMove.ordinal());
        this.zobristHash ^= ZobristKeys.getColorToMoveKey(this.colorToMove.getOpponent().ordinal());

        this.pieceGrid[from.x()][from.y()] = movingPiece;
        if (!move.isCapture()) {
            this.pieceGrid[to.x()][to.y()] = null;
        }
        this.pieceLocs.get(movingColor).add(from);
        this.pieceLocs.get(movingColor).remove(to);
        if (movingPiece.type().role().equals(PieceRole.ROOK_LIKE)) {
            this.castlingPartnerLocs.get(movingColor).add(from);
            this.castlingPartnerLocs.get(movingColor).remove(to);
        }
        this.zobristHash ^= ZobristKeys.getPieceSquareKey(movingPiece.type().id(), from.x(), from.y(),
                movingColor.ordinal());
        this.zobristHash ^= ZobristKeys.getPieceSquareKey(movingPiece.type().id(), to.x(), to.y(),
                movingColor.ordinal());

        if (move.getMovingPieceWasUnmoved()) {
            this.isUnmovedGrid[from.x()][from.y()] = true;
            this.zobristHash ^= ZobristKeys.getIsUnmovedKey(from.x(), from.y(), movingColor.ordinal());
        }

        if (movingPiece.type().role().equals(PieceRole.KING_LIKE)) {
            this.kingLocs.put(movingColor, from);
        }

        if (this.enPassantMoveTarget != null) {
            this.zobristHash ^= ZobristKeys.getEnPassantMoveTargetKey(this.enPassantMoveTarget.x(),
                    this.enPassantMoveTarget.y());
        }
        this.enPassantMoveTarget = this.enPassantMoveTargetHistory.pop();
        if (this.enPassantMoveTarget != null) {
            this.zobristHash ^= ZobristKeys.getEnPassantMoveTargetKey(this.enPassantMoveTarget.x(),
                    this.enPassantMoveTarget.y());
        }

        if (this.enPassantCaptureTarget != null) {
            this.zobristHash ^= ZobristKeys.getEnPassantMoveTargetKey(this.enPassantCaptureTarget.x(),
                    this.enPassantCaptureTarget.y());
        }
        this.enPassantCaptureTarget = this.enPassantCaptureTargetHistory.pop();
        if (this.enPassantCaptureTarget != null) {
            this.zobristHash ^= ZobristKeys.getEnPassantMoveTargetKey(this.enPassantCaptureTarget.x(),
                    this.enPassantCaptureTarget.y());
        }

        this.halfmoveClock = this.halfmoveClockHistory.pop();
        if (this.colorToMove.equals(Color.WHITE)) {
            this.fullmoveNumber--;
        }

        // --- CAPTURE MOVES ONLY ---

        if (move.isCapture()) {
            this.pieceGrid[move.getCapturedPieceLoc().x()][move.getCapturedPieceLoc().y()] = move.getCapturedPiece();
            if (!to.equals(move.getCapturedPieceLoc())) {
                this.pieceGrid[to.x()][to.y()] = null;
            }
            this.pieceLocs.get(move.getCapturedPiece().color()).add(move.getCapturedPieceLoc());

            if (move.getCapturedPiece().type().role().equals(PieceRole.ROOK_LIKE)) {
                this.castlingPartnerLocs.get(move.getCapturedPiece().color()).add(move.getCapturedPieceLoc());
            }
            this.zobristHash ^= ZobristKeys.getPieceSquareKey(move.getCapturedPiece().type().id(),
                    move.getCapturedPieceLoc().x(), move.getCapturedPieceLoc().y(),
                    move.getCapturedPiece().color().ordinal());

            if (move.getCapturedPieceWasUnmoved()) {
                this.isUnmovedGrid[move.getCapturedPieceLoc().x()][move.getCapturedPieceLoc().y()] = true;
                this.zobristHash ^= ZobristKeys.getIsUnmovedKey(move.getCapturedPieceLoc().x(),
                        move.getCapturedPieceLoc().y(), move.getCapturedPiece().color().ordinal());
            }
        }

        // --- PROMOTION MOVES ONLY ---

        if (move.isPromo()) {
            this.pieceGrid[from.x()][from.y()] = new Piece(move.getMovingPieceType(), movingColor);
            // TODO: Reflect if this is actually correct
            this.zobristHash ^= ZobristKeys.getPieceSquareKey(movingPiece.type().id(), to.x(), to.y(),
                    movingColor.ordinal());
            this.zobristHash ^= ZobristKeys.getPieceSquareKey(move.getPromoPieceType().id(), to.x(), to.y(),
                    movingColor.ordinal());
        }

        // --- CASTLING MOVES ONLY ---

        if (move.getIsCastling()) {
            Square castlingPartnerFrom = move.getTo();
            Square castlingPartnerTo;
            if (from.x() > castlingPartnerFrom.x()) {
                // Long castling
                castlingPartnerTo = getSquare(3, from.y());
            } else {
                // Short castling
                castlingPartnerTo = getSquare(this.width - 3, from.y());
            }
            Piece castlingPartnerPiece = getPieceAt(castlingPartnerTo);
            this.pieceGrid[castlingPartnerFrom.x()][castlingPartnerFrom.y()] = castlingPartnerPiece;
            this.pieceGrid[castlingPartnerTo.x()][castlingPartnerTo.y()] = null;
            this.pieceLocs.get(movingColor).add(castlingPartnerFrom);
            this.pieceLocs.get(movingColor).remove(castlingPartnerTo);
            this.castlingPartnerLocs.get(movingColor).add(castlingPartnerFrom);
            this.castlingPartnerLocs.get(movingColor).remove(castlingPartnerTo);
            this.zobristHash ^= ZobristKeys.getPieceSquareKey(castlingPartnerPiece.type().id(), castlingPartnerFrom.x(),
                    castlingPartnerFrom.y(), movingColor.ordinal());
            this.zobristHash ^= ZobristKeys.getPieceSquareKey(castlingPartnerPiece.type().id(), castlingPartnerTo.x(),
                    castlingPartnerTo.y(), movingColor.ordinal());
            this.isUnmovedGrid[castlingPartnerFrom.x()][castlingPartnerFrom.y()] = true;
            this.zobristHash ^= ZobristKeys.getIsUnmovedKey(castlingPartnerFrom.x(),
                    castlingPartnerFrom.y(), movingColor.ordinal());
        }
    }
}
