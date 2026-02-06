package com.chaoschess.backend.core.service;

import com.chaoschess.backend.api.BotConfigRequestDTO;
import com.chaoschess.backend.api.GameSettingsDTO;
import com.chaoschess.backend.api.GameStateDTO;
import com.chaoschess.backend.api.GameStateMapper;
import com.chaoschess.backend.api.GameStateMessagingController;
import com.chaoschess.backend.api.MoveRequestDTO;
import com.chaoschess.backend.core.ai.AlphaBetaSearcher;
import com.chaoschess.backend.core.ai.BoardEvaluator;
import com.chaoschess.backend.core.engine.board.Board;
import com.chaoschess.backend.core.engine.Move;
import com.chaoschess.backend.core.engine.RuleProcessor;
import com.chaoschess.backend.core.engine.board.BoardFactory;
import com.chaoschess.backend.core.engine.board.ChaosLevel;
import com.chaoschess.backend.core.model.GameOutcomeCategory;
import com.chaoschess.backend.core.model.ImmutableBoard;
import com.chaoschess.backend.core.model.Color;
import com.chaoschess.backend.core.model.GameOutcome;
import com.chaoschess.backend.core.model.Piece;
import com.chaoschess.backend.core.model.PieceType;
import com.chaoschess.backend.core.model.PieceTypes;
import com.chaoschess.backend.core.model.Square;
import com.chaoschess.backend.core.utils.BoardUtils;
import com.chaoschess.backend.core.utils.ZobristKeys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
public class GameManager implements ApplicationContextAware {

    private static final int AI_SEARCH_DEPTH = 3;
//    private static final int AI_SEARCH_DEPTH = 4;

    private final PieceTypes pieceTypes;
    private final RuleProcessor ruleProcessor;
    private final GameStateMapper gameStateMapper;
    private final GameStateMessagingController messagingController;
    private final Deque<ImmutableBoard> immutableBoardHistory;
    private final Deque<Move> moveHistory;
    private final String initialFenPosition;

    private Board board;
    private ImmutableBoard immutableBoard;
    private List<Move> pseudolegalMoves;
    private List<Move> legalMoves;
    private GameOutcome gameOutcome;

    private final AlphaBetaSearcher alphaBetaSearcher;
    private final BoardEvaluator boardEvaluator;
    private ExecutorService aiSearchExecutor;
    private Set<Color> botColors;
    // volatile guarantees visibility of updates across threads (bypasses CPU cache, this is required here)
    private volatile boolean isAiSearching;
    private ApplicationContext applicationContext;

    @Autowired
    public GameManager(PieceTypes pieceTypes,
                       RuleProcessor ruleProcessor,
                       GameStateMapper gameStateMapper,
                       GameStateMessagingController messagingController,
                       AlphaBetaSearcher alphaBetaSearcher,
                       BoardEvaluator boardEvaluator,
                       @Value("${game.initial-fen:"
                               + "#{T(com.chaoschess.backend.core.engine.board.Board).STANDARD_INITIAL_BOARD_FEN}}")
                       String initialFenPosition) {
        this.pieceTypes = pieceTypes;
        this.ruleProcessor = ruleProcessor;
        this.gameStateMapper = gameStateMapper;
        this.messagingController = messagingController;
        this.immutableBoardHistory = new ArrayDeque<>();
        this.moveHistory = new ArrayDeque<>();
        this.initialFenPosition = initialFenPosition;

        this.alphaBetaSearcher = alphaBetaSearcher;
        this.boardEvaluator = boardEvaluator;

//        this.botColors = Set.of();
//        this.botColors = Set.of(Color.BLACK);
        this.botColors = Set.of(Color.WHITE, Color.BLACK);

        this.isAiSearching = false;

        ZobristKeys.initializeKeys(16, 16, pieceTypes.pieceTypesMap().size());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.aiSearchExecutor = createNewAiSearchExecutor();
    }

    @PostConstruct
    private void initBoardAndStartGame() {
        long seed = new Random().nextLong();
        this.board = BoardFactory.createRandomInitialBoard(ChaosLevel.DULL, seed, this.pieceTypes);

        this.boardEvaluator.initializePSTs(this.board.getWidth(), this.board.getHeight(), this.pieceTypes);

        this.pseudolegalMoves = new ArrayList<>();
        this.legalMoves = new ArrayList<>();

        executeStateChange();
    }

    private ExecutorService createNewAiSearchExecutor() {
        return (ExecutorService) applicationContext.getBean("aiSearchExecutor");
    }

    public Board getBoard() {
        return this.board;
    }

    public void setBotConfig(BotConfigRequestDTO botConfigRequestDTO) {
        this.botColors = botConfigRequestDTO.botColorNames().stream()
                .map(Color::valueOf)
                .collect(Collectors.toSet());
    }

    public void startNewGame(GameSettingsDTO gameSettingsDTO) {
        this.immutableBoardHistory.clear();
        this.moveHistory.clear();

        ChaosLevel chaosLevel = ChaosLevel.getByInt(gameSettingsDTO.chaosLevel());
        long seed = new Random().nextLong();
        this.board = BoardFactory.createRandomInitialBoard(chaosLevel, seed, this.pieceTypes);

        this.boardEvaluator.initializePSTs(this.board.getWidth(), this.board.getHeight(), this.pieceTypes);

        executeStateChange();
    }

    // TODO: Find better names for this method and for the methods it calls
    private void executeStateChange() {
        if (this.isAiSearching) {
            this.aiSearchExecutor.shutdownNow();
            this.aiSearchExecutor = createNewAiSearchExecutor();
            this.isAiSearching = false;
        }
        updateState();
        propagateNewState();
        handleNewState();
    }

    private void updateState() {
        this.immutableBoard = createImmutableBoardFromBoard(this.board);

        // legalMoves is a subset of pseudolegalMoves, so releasing all pseudolegal Moves is sufficient
        this.ruleProcessor.getMovePool().releaseAllMoves(this.pseudolegalMoves);

        this.pseudolegalMoves = this.ruleProcessor.calculatePseudolegalMoves(this.board);
        this.legalMoves = this.ruleProcessor.calculateLegalMoves(this.board, this.pseudolegalMoves, false);

        this.gameOutcome = this.ruleProcessor.determineGameOutcome(this.board, this.legalMoves);

//        System.out.printf("MovePool currentMovesUsed - maxMovesUsed - movesReturned: %,d - %,d - %,d%n",
//                MovePool.currentMovesUsed, MovePool.maxMovesUsed, MovePool.movesReturned);
    }

    public void propagateNewState() {
        // TODO: Clarify that ... == null indicates a standalone test environment (bypassing Spring and frontend)
        if (this.messagingController != null) {
            this.messagingController.pushGameState(this.getGameStateDTO());
        }
    }

    public void handleNewState() {
        if (this.gameOutcome.getCategory() == GameOutcomeCategory.ONGOING) {
            if (isBotTurn()) {
                handleAiTurnAsync();
            }
        } else {
            System.out.println("Game over: " + this.gameOutcome.state());
        }
    }

    private boolean isBotTurn() {
        return this.botColors.contains(this.board.getColorToMove());
    }

    public RuleProcessor getMoveGenerator() {
        return this.ruleProcessor;
    }

    public List<Move> getLegalMoves() {
        return Collections.unmodifiableList(this.legalMoves);
    }

    private Move getLegalMoveFromMoveRequest(MoveRequestDTO moveRequestDTO) {
        Square from = BoardUtils.squareIndexToSquare(moveRequestDTO.fromSquareIndex(), this.board.getWidth(),
                this.board.getHeight());
        Square to = BoardUtils.squareIndexToSquare(moveRequestDTO.toSquareIndex(), this.board.getWidth(),
                this.board.getHeight());
        PieceType promotionPieceType = this.pieceTypes.getPieceTypeByName(moveRequestDTO.promoPieceName());

        if (!(this.board.isInBounds(from) && this.board.isInBounds(to) && (this.board.getPieceAt(from) != null))) {
            return null;
        } else {
            return this.legalMoves.stream()
                    .filter(move -> move.getFrom().equals(from)
                            && move.getTo().equals(to)
                            && Objects.equals(move.getPromoPieceType(), promotionPieceType))
                    .findFirst().orElse(null);
        }
    }

    public void tryAndExecutePlayerMoveRequest(MoveRequestDTO moveRequestDTO) {
        if (isBotTurn()) {
            System.err.println("Move request ignored because it is the bot's turn.");
            return;
        }

        Move legalMove = getLegalMoveFromMoveRequest(moveRequestDTO);
        if (legalMove != null) {
            executeMove(legalMove);
        } else {
            throw new IllegalArgumentException();
        }
    }

    // TODO: this method should be private, but it is needed for perf-testing the GameManager via Main at the moment
    public void executeMove(Move legalMove) {
        this.immutableBoardHistory.push(this.immutableBoard);
        this.moveHistory.push(legalMove);

        this.board.makeMove(legalMove);
        executeStateChange();
    }

    public void undoMove() {
        if (!canUndoMove()) {
            throw new IllegalStateException("Cannot undo move. History is empty.");
        }
        this.immutableBoardHistory.pop();
        this.board.unmakeMove(this.moveHistory.pop());
        executeStateChange();
    }

    public boolean canUndoMove() {
        return !this.moveHistory.isEmpty();
    }

    public GameStateDTO getGameStateDTO() {
        return this.gameStateMapper.toDTO(this.immutableBoard, this.pseudolegalMoves, this.legalMoves,
                this.gameOutcome, this.botColors);
    }

    private ImmutableBoard createImmutableBoardFromBoard(Board board) {
        int width = board.getWidth();
        int height = board.getHeight();

        Piece[][] pieceGrid = new Piece[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // TODO: Flat copies of the inner arrays should suffice here due to Piece being immutable
                Piece piece = board.getPieceAt(x, y);
                if (piece != null) {
                    pieceGrid[x][y] = new Piece(piece.type(), piece.color());
                } else {
                    pieceGrid[x][y] = null;
                }
            }
        }

        Map<Color, Set<Square>> castlingPartnerLocs = new EnumMap<>(Color.class);
        for (Color color : Color.values()) {
            Set<Square> copiedSet = new HashSet<>(board.getCastlingPartnerLocs().get(color));
            Set<Square> unmodifiableSet = Collections.unmodifiableSet(copiedSet);
            castlingPartnerLocs.put(color, unmodifiableSet);
        }

        return new ImmutableBoard(width, height, pieceGrid, castlingPartnerLocs, board.getPromoOptions(),
                board.getColorToMove(), board.getEnPassantMoveTarget(), board.getHalfmoveClock(),
                board.getFullmoveNumber());
    }

    private void handleAiTurnAsync() {
        if (this.isAiSearching) {
            System.out.println("AI search is already running. Ignoring AI search start request.");
            return;
        }
        this.isAiSearching = true;

        Board boardCopy = this.board.deepCopy();

        long aiSearchStartTime = System.nanoTime();

        aiSearchExecutor.submit(() -> {
            Move bestMove = null;
            try {
                bestMove = alphaBetaSearcher.findBestMove(boardCopy, AI_SEARCH_DEPTH);
            } catch (InterruptedException e) {
                System.out.println("AI search canceled. Reason: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Critical error during AI search: " + e.getMessage());
            } finally {
                long aiSearchDurationNanosecs = System.nanoTime() - aiSearchStartTime;
                double aiSearchDurationSecs = aiSearchDurationNanosecs / 1_000_000_000.0;
                long aiSearchBoardsEvaluated = this.alphaBetaSearcher.getBoardsEvaluatedPerMove();
                double aiSearchBoardsEvaluatedPerSec = aiSearchBoardsEvaluated / aiSearchDurationSecs;
                long aiSearchLeafNodesEvaluated = this.alphaBetaSearcher.getLeafNodesEvaluatedPerMove();
                double aiSearchLeafNodesEvaluatedPerSec = aiSearchLeafNodesEvaluated / aiSearchDurationSecs;
                System.out.println("=======================");
                System.out.printf("AI STATS - Search duration: %.2fs | Boards evaluated: %d | Boards per second: %.2f" +
                                " | Leaf nodes evaluated: %d | Leaf nodes per second: %.2f%n",
                        aiSearchDurationSecs, aiSearchBoardsEvaluated, aiSearchBoardsEvaluatedPerSec,
                        aiSearchLeafNodesEvaluated, aiSearchLeafNodesEvaluatedPerSec);

                this.isAiSearching = false;
                if (bestMove != null) {
                    System.out.println("AI move played: " + BoardUtils.moveToNotation(bestMove));
                    executeMove(bestMove);
                } else {
                    System.err.println("No AI move found. Checkmate or stalemate?");
                }
            }
        });
    }
}
