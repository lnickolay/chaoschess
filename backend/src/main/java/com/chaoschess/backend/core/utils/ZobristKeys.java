package com.chaoschess.backend.core.utils;

import com.chaoschess.backend.core.model.Color;

import java.security.SecureRandom;
import java.util.Random;

public final class ZobristKeys {

    private static long[][][][] PIECE_SQUARE_KEYS;
    private static long[][][] IS_UNMOVED_KEYS;
    private static long[] COLOR_TO_MOVE_KEYS;
    private static long[][] EN_PASSANT_MOVE_TARGET_KEYS;
    private static long[][] EN_PASSANT_CAPTURE_TARGET_KEYS;

    private static volatile boolean initialized = false;

    private static final Random RANDOM = new SecureRandom();

    private ZobristKeys() {}

    public static long getPieceSquareKey(int pieceTypeID, int x, int y, int colorID) {
        if (!initialized) {
            throw new IllegalStateException("Zobrist keys have not been initialized yet.");
        }
        return PIECE_SQUARE_KEYS[pieceTypeID][x][y][colorID];
    }

    // TODO: Understand why it is apparently necessary to include the color here
    public static long getIsUnmovedKey(int x, int y, int colorID) {
        if (!initialized) {
            throw new IllegalStateException("Zobrist keys have not been initialized yet.");
        }
        return IS_UNMOVED_KEYS[x][y][colorID];
    }

    public static long getColorToMoveKey(int colorID) {
        if (!initialized) {
            throw new IllegalStateException("Zobrist keys have not been initialized yet.");
        }
        return COLOR_TO_MOVE_KEYS[colorID];
    }

    public static long getEnPassantMoveTargetKey(int x, int y) {
        if (!initialized) {
            throw new IllegalStateException("Zobrist keys have not been initialized yet.");
        }
        return EN_PASSANT_MOVE_TARGET_KEYS[x][y];
    }

    public static long getEnPassantCaptureTargetKey(int x, int y) {
        if (!initialized) {
            throw new IllegalStateException("Zobrist keys have not been initialized yet.");
        }
        return EN_PASSANT_CAPTURE_TARGET_KEYS[x][y];
    }

    public static void initializeKeys(int maxWidth, int maxHeight, int pieceTypeCount) {
        // TODO: Gain a better understanding of the Double-Checked Locking (DCL) used here
        if (initialized) {
            return;
        }
        synchronized (ZobristKeys.class) {
            if (initialized) {
                return;
            }

            PIECE_SQUARE_KEYS = new long[pieceTypeCount][maxWidth][maxHeight][Color.values().length];
            for (int pieceTypeID = 0; pieceTypeID < pieceTypeCount; pieceTypeID++) {
                for (int x = 0; x < maxWidth; x++) {
                    for (int y = 0; y < maxHeight; y++) {
                        for (int colorID = 0; colorID < Color.values().length; colorID++) {
                            PIECE_SQUARE_KEYS[pieceTypeID][x][y][colorID] = RANDOM.nextLong();
                        }
                    }
                }
            }

            IS_UNMOVED_KEYS = new long[maxWidth][maxHeight][Color.values().length];
            for (int x = 0; x < maxWidth; x++) {
                for (int y = 0; y < maxHeight; y++) {
                    for (int colorID = 0; colorID < Color.values().length; colorID++) {
                        IS_UNMOVED_KEYS[x][y][colorID] = RANDOM.nextLong();
                    }
                }
            }

            COLOR_TO_MOVE_KEYS = new long[Color.values().length];
            for (int colorID = 0; colorID < Color.values().length; colorID++) {
                COLOR_TO_MOVE_KEYS[colorID] = RANDOM.nextLong();
            }

            EN_PASSANT_MOVE_TARGET_KEYS = new long[maxWidth][maxHeight];
            EN_PASSANT_CAPTURE_TARGET_KEYS = new long[maxWidth][maxHeight];
            for (int x = 0; x < maxWidth; x++) {
                for (int y = 0; y < maxHeight; y++) {
                    EN_PASSANT_MOVE_TARGET_KEYS[x][y] = RANDOM.nextLong();
                    EN_PASSANT_CAPTURE_TARGET_KEYS[x][y] = RANDOM.nextLong();
                }
            }

            initialized = true;
        }
    }
}
