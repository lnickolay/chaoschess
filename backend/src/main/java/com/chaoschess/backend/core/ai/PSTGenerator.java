package com.chaoschess.backend.core.ai;

import com.chaoschess.backend.core.model.PieceType;
import com.chaoschess.backend.core.model.PieceTypes;
import com.chaoschess.backend.core.model.Square;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PSTGenerator {

    private PSTGenerator() {}

    public static PSTData generatePSTs(int width, int height, PieceTypes pieceTypes) {
        // --- REGULAR PIECES ---

        int[][] kingMidgamePST = new int[width][height];
        int[][] kingEndgamePST = new int[width][height];
        int[][] queenPST = new int[width][height];
        int[][] rookPST = new int[width][height];
        int[][] bishopPST = new int[width][height];
        int[][] knightPST = new int[width][height];
        int[][] pawnMidgamePST = new int[width][height];
        int[][] pawnEndgamePST = new int[width][height];

        int[][] mhDistFromCenter = generateDistFromCenterArray(width, height, DistanceMetric.MANHATTAN);
        double[][] mhDistFromCenterNormalized = normalize2DArray(mhDistFromCenter);
        double[][] chebDistFromCenterNormalized = normalize2DArray(generateDistFromCenterArray(
                width, height, DistanceMetric.CHEBYSHEV));

        double[][] mhDistFromOwnHomeRankNormalized = normalize2DArray(generateDistFromRanksArray(
                width, height, List.of(0), DistanceMetric.MANHATTAN));

        int[][] mhDistFromEnemyPawnRank = generateDistFromRanksArray(
                width, height, List.of(height - 2), DistanceMetric.MANHATTAN);
        double[][] mhDistFromEnemyPawnRankNormalized = normalize2DArray(mhDistFromEnemyPawnRank);

        int[][] mhDistFromEdgeFiles = generateDistFromFilesArray(
                width, height, List.of(0, width - 1), DistanceMetric.MANHATTAN);

        int[][] mhDistFromCentralHomeRank = generateDistFromTargetsArray(
                width, height, getCentralHomeRankSquares(width, height), DistanceMetric.MANHATTAN);

        List<Square> safeKingLocsSquares = getSafeKingLocsSquares(width, height);
        int[][] chebDistFromSafeKingLocs = generateDistFromTargetsArray(
                width, height, safeKingLocsSquares, DistanceMetric.CHEBYSHEV);
        double[][] mhDistFromSafeKingLocsNormalized = normalize2DArray(generateDistFromTargetsArray(
                width, height, safeKingLocsSquares, DistanceMetric.MANHATTAN));

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                kingMidgamePST[x][y] = (int) (30 - 50 * Math.pow(mhDistFromSafeKingLocsNormalized[x][y], 0.5)
                        - 30 * Math.pow(mhDistFromOwnHomeRankNormalized[x][y], 0.5));
                kingEndgamePST[x][y] = (int) (40 - 80 * mhDistFromCenterNormalized[x][y]);
                queenPST[x][y] = (int) (10 - 30 * Math.pow(mhDistFromCenterNormalized[x][y], 2.0));
                rookPST[x][y] = ((mhDistFromEnemyPawnRank[x][y] == 0) ? 10 : 0)
                        - ((mhDistFromEdgeFiles[x][y] == 0) ? 5 : 0);
                bishopPST[x][y] = (int) (10 - 30 * Math.pow(chebDistFromCenterNormalized[x][y], 2.0));
                knightPST[x][y] = (int) (20 - 60 * mhDistFromCenterNormalized[x][y]);
                pawnMidgamePST[x][y] = ((mhDistFromEnemyPawnRank[x][y] == 0) ? 50 : 0)
                        + ((mhDistFromEnemyPawnRank[x][y] == 1) ? 20 : 0)
                        + ((chebDistFromSafeKingLocs[x][y] <= 1) ? 20 : 0)
                        + ((mhDistFromCenter[x][y] == 0) ? 20 : 0)
                        + ((mhDistFromCenter[x][y] == 1) ? 10 : 0)
                        - ((mhDistFromCentralHomeRank[x][y] <= 1) ? 20 : 0);
                pawnEndgamePST[x][y] = (int) (150 - 180 * Math.pow(mhDistFromEnemyPawnRankNormalized[x][y], 0.33));
            }
        }

        Map<PieceType, int[][]> midgamePSTs = new HashMap<>();
        midgamePSTs.put(pieceTypes.getPieceTypeByName("King"), kingMidgamePST);
        midgamePSTs.put(pieceTypes.getPieceTypeByName("Queen"), queenPST);
        midgamePSTs.put(pieceTypes.getPieceTypeByName("Rook"), rookPST);
        midgamePSTs.put(pieceTypes.getPieceTypeByName("Bishop"), bishopPST);
        midgamePSTs.put(pieceTypes.getPieceTypeByName("Knight"), knightPST);
        midgamePSTs.put(pieceTypes.getPieceTypeByName("Pawn"), pawnMidgamePST);

        Map<PieceType, int[][]> endgamePSTs = new HashMap<>();
        endgamePSTs.put(pieceTypes.getPieceTypeByName("King"), kingEndgamePST);
        endgamePSTs.put(pieceTypes.getPieceTypeByName("Pawn"), pawnEndgamePST);
        for (String pieceName : List.of("Queen", "Rook", "Bishop", "Knight")) {
            PieceType pieceType = pieceTypes.getPieceTypeByName(pieceName);
            endgamePSTs.put(pieceType, midgamePSTs.get(pieceType));
        }

        // --- FAIRY CHESS PIECES ---
        // TODO: Maybe add custom PSTs for fairy chess pieces

        // Set knight PST for all short range / leaper pieces
        for (String pieceName : List.of("Wazir", "Ferz", "Alfil", "Zebra", "Giraffe", "Mann", "Centaur")) {
            PieceType pieceType = pieceTypes.getPieceTypeByName(pieceName);
            if (pieceType != null) {
                midgamePSTs.put(pieceType, queenPST);
                endgamePSTs.put(pieceType, queenPST);
            }
        }

        // Set queen PST for all long range pieces
        for (String pieceName : List.of("Archbishop", "Chancellor", "Amazon", "Nightrider")) {
            PieceType pieceType = pieceTypes.getPieceTypeByName(pieceName);
            if (pieceType != null) {
                midgamePSTs.put(pieceType, queenPST);
                endgamePSTs.put(pieceType, queenPST);
            }
        }

        return new PSTData(midgamePSTs, endgamePSTs);
    }

    private static double[][] normalize2DArray(int[][] array) {
        int arrayWidth = array.length;
        int arrayHeight = array[0].length;

        double[][] normalizedArray = new double[arrayWidth][arrayHeight];

        int minValue = Arrays.stream(array).flatMapToInt(Arrays::stream).min().orElse(0);
        int maxValue = Arrays.stream(array).flatMapToInt(Arrays::stream).max().orElse(0);

        int valueRange = maxValue - minValue;

        if (valueRange != 0) {
            for (int x = 0; x < arrayWidth; x++) {
                for (int y = 0; y < arrayHeight; y++) {
                    normalizedArray[x][y] = ((double) array[x][y] - minValue) / valueRange;
                }
            }
        }
        return normalizedArray;
    }

    private static int[][] generateDistFromCenterArray(int width, int height, DistanceMetric metric) {
        return generateDistFromTargetsArray(width, height, getCenterSquares(width, height), metric);
    }

    private static int[][] generateDistFromRanksArray(int width, int height, List<Integer> ranks,
                                                      DistanceMetric metric) {
        return generateDistFromTargetsArray(width, height, getRowsSquares(width, height, ranks), metric);
    }

    private static int[][] generateDistFromFilesArray(int width, int height, List<Integer> files,
                                                      DistanceMetric metric) {
        return generateDistFromTargetsArray(width, height, getColsSquares(width, height, files), metric);
    }

    private static int[][] generateDistFromTargetsArray(int width, int height, List<Square> targets,
                                                        DistanceMetric metric) {
        int[][] minManhattanDistArray = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int minDist = Integer.MAX_VALUE;
                for (Square target : targets) {
                    int xDist = Math.abs(x - target.x());
                    int yDist = Math.abs(y - target.y());
                    int targetDist;
                    if (metric == DistanceMetric.MANHATTAN) {
                        targetDist = xDist + yDist;
                    } else {
                        targetDist = Math.max(xDist, yDist);
                    }
                    minDist = Math.min(minDist, targetDist);
                }
                minManhattanDistArray[x][y] = minDist;
            }
        }
        return minManhattanDistArray;
    }

    private static List<Square> getCenterSquares(int width, int height) {
        int minCenterX = (width - 1) / 2;
        int maxCenterX = width / 2;
        int minCenterY = (height - 1) / 2;
        int maxCenterY = height / 2;

        List<Square> centerSquares = new ArrayList<>();
        for (int x = minCenterX; x <= maxCenterX; x++) {
            for (int y = minCenterY; y <= maxCenterY; y++) {
                centerSquares.add(new Square(x, y));
            }
        }
        return centerSquares;
    }

    private static List<Square> getRowsSquares(int width, int height, List<Integer> rows) {
        List<Square> rowsSquares = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y : rows) {
                rowsSquares.add(new Square(x, y));
            }
        }
        return rowsSquares;
    }

    private static List<Square> getColsSquares(int width, int height, List<Integer> cols) {
        List<Square> colsSquares = new ArrayList<>();
        for (int x : cols) {
            for (int y = 0; y < width; y++) {
                colsSquares.add(new Square(x, y));
            }
        }
        return colsSquares;
    }

    private static List<Square> getSafeKingLocsSquares(int width, int height) {
        int maxXEdgeDist = (width <= 7) ? 0 : 1;
        List<Square> safeKingLocsSquares = new ArrayList<>();
        for (int x = 0; x < maxXEdgeDist + 1; x++) {
            safeKingLocsSquares.add(new Square(x, 0));
            safeKingLocsSquares.add(new Square((width - 1) - x, 0));
        }
        return safeKingLocsSquares;
    }

    private static List<Square> getCentralHomeRankSquares(int width, int height) {
        assert width >= 3;
        int minXEdgeDist = Math.min((width + 1) / 3, 3);
        List<Square> centralHomeRankSquares = new ArrayList<>();
        for (int x = minXEdgeDist; x < width - minXEdgeDist; x++) {
            centralHomeRankSquares.add(new Square(x, 0));
        }
        return centralHomeRankSquares;
    }

    public static void print2DArray(int[][] array) {
        for (int y = array[0].length - 1; y >= 0; y--) {
            for (int x = 0; x < array.length; x++) {
                System.out.printf("%3d ", array[x][y]);
            }
            System.out.println();
        }
        System.out.println();
    }

    public static void print2DArray(double[][] array) {
        for (int y = array[0].length - 1; y >= 0; y--) {
            for (int x = 0; x < array.length; x++) {
                System.out.printf("%.2f ", array[x][y]);
            }
            System.out.println();
        }
        System.out.println();
    }

//    // TODO: Remove once no longer needed for testing board.isUnmovedGrid
//    public static void print2DArray(boolean[][] array) {
//        for (int y = array[0].length - 1; y >= 0; y--) {
//            for (int x = 0; x < array.length; x++) {
//                System.out.printf("%b ", array[x][y]);
//            }
//            System.out.println();
//        }
//        System.out.println();
//    }
}
