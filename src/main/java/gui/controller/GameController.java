package gui.controller;

import board.Position;
import chess.ChessException;
import chess.ai.BotEngine;
import chess.ai.StockfishEngine;
import chess.ai.UCIMapper;
import chess.core.ChessMatch;
import chess.core.ChessPiece;
import chess.core.ChessPosition;
import chess.game.GameConfig;
import chess.game.GameManager;
import chess.game.GameSession;
import chess.game.GameState;
import chess.history.Move;
import chess.service.BotMoveService;
import gui.service.GameClockController;
import gui.service.MoveHistoryFormatter;
import gui.service.MoveSoundService;
import gui.service.NavigationService;
import gui.service.PieceAnimator;
import gui.view.ChessBoardView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class GameController {

    private static final Logger LOG = Logger.getLogger(GameController.class.getName());
    private static final String COORD_STYLE = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;";
    private static final long MIN_BOT_THINKING_TIME_MS = 1_500L;

    // --- FXML Elements ---
    @FXML private Label whiteTimeLabel;
    @FXML private Label blackTimeLabel;
    @FXML private GridPane boardGrid;
    @FXML private Label turnLabel;
    @FXML private Label statusLabel;
    @FXML private ListView<String> movesList;

    // --- Core Chess Objects ---
    private ChessMatch chessMatch;
    private GameConfig gameConfig;
    private GameSession session;
    private GameManager gameManager;

    // --- UI Context State ---
    private ChessBoardView chessBoardView;
    private Position sourcePosition;
    private boolean[][] possibleMoves;
    private boolean botUnavailable;

    // --- Delegated Services (SRP) ---
    private PieceAnimator pieceAnimator;
    private BotMoveService botMoveService;
    private MoveSoundService moveSoundService;
    private GameClockController clockController;
    private MoveHistoryFormatter historyFormatter;
    private NavigationService navigationService;

    @FXML
    public void initialize() {
        this.chessBoardView = new ChessBoardView();
        this.pieceAnimator = new PieceAnimator();
        this.moveSoundService = new MoveSoundService();
        this.clockController = new GameClockController();
        this.historyFormatter = new MoveHistoryFormatter();
        this.navigationService = new NavigationService();
    }

    public void setup(GameConfig config) {
        this.gameConfig = config;

        BotEngine engine = createBotEngine(config);
        this.gameManager = new GameManager(config, engine);
        this.session = new GameSession(gameManager, config);
        this.chessMatch = gameManager.getMatch();
        this.botMoveService = new BotMoveService(engine);

        clearSelection();
        this.session.start();

        if (config.hasTimedGame()) {
            this.clockController.start(session, this::updateTimerUI);
        } else {
            whiteTimeLabel.setText("--:--");
            blackTimeLabel.setText("--:--");
        }

        if (engine != null) {
            try {
                engine.start();
                LOG.info("Stockfish pronto para uso.");
            } catch (IOException e) {
                this.botUnavailable = true;
                LOG.severe("Falha crítica no Stockfish: " + e.getMessage());
                statusLabel.setText("IA indisponível: " + e.getMessage());
            }
        }

        drawBoard();
        updateUI();
        updateMoveHistory();
        checkInitialBotMove();
    }

    private void checkInitialBotMove() {
        if (gameConfig.isBotTurn(chessMatch.getCurrentPlayer())) {
            scheduleBotMove();
        }
    }

    // --- Board Rendering (UI Responsibility) ---

    private void drawBoard() {
        boardGrid.getChildren().clear();
        ChessPiece[][] pieces = chessMatch.getPieces();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = pieces[row][col];
                boolean isSource = sourcePosition != null && sourcePosition.getRow() == row && sourcePosition.getColumn() == col;
                boolean isPossible = possibleMoves != null && possibleMoves[row][col];
                boolean isCapture = isPossible && piece != null;

                StackPane square = chessBoardView.createSquare(row, col, piece, isSource, isPossible, isCapture);

                final int r = row, c = col;
                square.setOnMouseClicked(e -> handleSquareClick(r, c));
                boardGrid.add(square, col + 1, row);
            }
        }
        addCoordinates();
    }

    private void addCoordinates() {
        for (int row = 0; row < 8; row++) {
            Label rank = new Label(String.valueOf(8 - row));
            rank.setStyle(COORD_STYLE);
            rank.setAlignment(Pos.CENTER_RIGHT);
            rank.setPadding(new Insets(0, 6, 0, 0));
            boardGrid.add(rank, 0, row);
        }
        for (int col = 0; col < 8; col++) {
            Label file = new Label(String.valueOf((char) ('a' + col)));
            file.setStyle(COORD_STYLE);
            file.setAlignment(Pos.CENTER);
            file.setPadding(new Insets(4, 0, 0, 0));
            boardGrid.add(file, col + 1, 8);
        }
    }

    // --- Input Handling ---

    private void handleSquareClick(int row, int col) {
        if (session != null && !session.getCurrentState().canPlayerMove()) {
            statusLabel.setText(session.getCurrentState().getStatusMessage());
            return;
        }

        Position clickedPos = new Position(row, col);

        try {
            if (sourcePosition == null || isAllyPiece(row, col)) {
                handleSelection(clickedPos);
            } else if (sourcePosition.equals(clickedPos)) {
                clearSelection();
            } else {
                executeMove(clickedPos);
                return; // O executeMove gerencia o próprio ciclo de drawBoard
            }
            drawBoard();
        } catch (ChessException e) {
            statusLabel.setText("Erro: " + e.getMessage());
            clearSelection();
            drawBoard();
        }
        updateMoveHistory();
    }

    private void handleSelection(Position pos) {
        ChessPiece piece = chessMatch.getPieces()[pos.getRow()][pos.getColumn()];
        if (piece == null) return;

        if (piece.getColor() != chessMatch.getCurrentPlayer()) {
            statusLabel.setText("Essa peça não é do jogador atual.");
            return;
        }

        sourcePosition = pos;
        possibleMoves = chessMatch.possibleMoves(ChessPosition.fromPosition(pos));
        statusLabel.setText("Selecionado: " + getFriendlyPieceName(piece) + " em " + ChessPosition.fromPosition(pos));
    }

    private void executeMove(Position targetPos) {
        Position animatedSource = sourcePosition;
        Position animatedTarget = targetPos;
        ChessPiece animatedPiece = chessMatch.getPieces()[animatedSource.getRow()][animatedSource.getColumn()];

        ChessPosition source = ChessPosition.fromPosition(sourcePosition);
        ChessPosition target = ChessPosition.fromPosition(targetPos);
        boolean wasCapture = chessMatch.getPieces()[targetPos.getRow()][targetPos.getColumn()] != null;

        chessMatch.performChessMove(source, target);
        boolean wasPromotion = chessMatch.getPromoted() != null;

        if (wasPromotion) {
            handlePromotion();
        }

        moveSoundService.playMoveSound(gameConfig.isSoundEnabled(), chessMatch.getCheckMate(),
                chessMatch.getCheck(), wasPromotion, wasCapture);

        session.updateClock();
        clearSelection();
        updateUI();
        updateGameStateAfterMove();

        drawBoard();
        pieceAnimator.animateMove(boardGrid, chessBoardView, animatedPiece, animatedSource, animatedTarget, () -> {
            drawBoard();
            if (!chessMatch.getCheckMate() && gameConfig.isBotTurn(chessMatch.getCurrentPlayer())) {
                scheduleBotMove();
            }
        });
    }

    private void updateGameStateAfterMove() {
        if (chessMatch.getCheckMate()) {
            session.setCurrentState(GameState.CHECKMATE);
            statusLabel.setText("Fim de jogo: Xeque-mate!");
        } else if (gameConfig.isBotTurn(chessMatch.getCurrentPlayer())) {
            session.setCurrentState(GameState.BOT_THINKING);
            statusLabel.setText("IA está calculando...");
        } else {
            session.setCurrentState(chessMatch.getCheck() ? GameState.CHECK : GameState.RUNNING);
        }
    }

    private void handlePromotion() {
        String choice = askPromotionPiece();
        chessMatch.replacePromotedPiece(choice);
        List<Move> moves = chessMatch.getChessLog().getMoves();
        Move lastMove = moves.get(moves.size() - 1);
        lastMove.setSanAnnotation(lastMove.getSanAnnotation() + "=" + choice);
    }

    // --- AI Integration (Orchestration) ---

    private void scheduleBotMove() {
        if (!botMoveService.isEngineReady()) {
            statusLabel.setText("IA indisponível ou não configurada.");
            session.setCurrentState(GameState.RUNNING);
            return;
        }

        String history = historyFormatter.buildUciHistory(chessMatch.getChessLog().getMoves());

        botMoveService.requestMove(
                        history,
                        gameConfig.getDifficulty(),
                        chessMatch.getCurrentPlayer()
                )
                .thenAccept(uciMoveOpt -> Platform.runLater(() ->
                        uciMoveOpt.ifPresentOrElse(
                                this::applyBotMove,  // ✅ Agora vai chamar o método correto
                                () -> {
                                    statusLabel.setText("Erro: IA não retornou um lance.");
                                    session.setCurrentState(GameState.RUNNING);
                                }
                        )
                ))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Erro ao processar lance da IA.");
                        session.setCurrentState(GameState.RUNNING);
                        LOG.warning("Exceção no scheduleBotMove: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void applyBotMove(String uciMove) {
        try {
            ChessPosition source = UCIMapper.source(uciMove);
            ChessPosition target = UCIMapper.target(uciMove);

            Position srcPos = source.toPosition();
            Position tgtPos = target.toPosition();
            ChessPiece animatedPiece = chessMatch.getPieces()[srcPos.getRow()][srcPos.getColumn()];
            boolean wasCapture = chessMatch.getPieces()[tgtPos.getRow()][tgtPos.getColumn()] != null;

            chessMatch.performChessMove(source, target);
            boolean wasPromotion = chessMatch.getPromoted() != null;

            if (wasPromotion) {
                String piece = uciMove.length() == 5
                        ? String.valueOf(uciMove.charAt(4)).toUpperCase()
                        : "Q";
                chessMatch.replacePromotedPiece(piece);
            }

            moveSoundService.playMoveSound(
                    gameConfig.isSoundEnabled(),
                    chessMatch.getCheckMate(),
                    chessMatch.getCheck(),
                    wasPromotion,
                    wasCapture
            );

            updateGameStateAfterMove();
            drawBoard();

            pieceAnimator.animateMove(boardGrid, chessBoardView, animatedPiece, srcPos, tgtPos, () -> {
                drawBoard();
                updateUI();
                updateMoveHistory();
                if (!chessMatch.getCheckMate() && gameConfig.isBotTurn(chessMatch.getCurrentPlayer())) {
                    scheduleBotMove();
                }
            });
        } catch (Exception e) {
            LOG.warning("Erro ao mapear lance do Bot: " + e.getMessage());
            session.setCurrentState(GameState.RUNNING);
            drawBoard();
            updateUI();
        }
    }

    // --- UI Synchronizers ---

    private void updateTimerUI() {
        whiteTimeLabel.setText(session.getFormattedWhiteTime());
        blackTimeLabel.setText(session.getFormattedBlackTime());

        if (session.getCurrentState() == GameState.TIMEOUT) {
            statusLabel.setText("FIM DE TEMPO!");
            drawBoard(); // Bloqueia interações visuais indiretamente
        }
    }

    private void updateUI() {
        turnLabel.setText(chessMatch.getCurrentPlayer().toString());
        if (botUnavailable) {
            statusLabel.setText("IA indisponível. Verifique o caminho do Stockfish.");
        } else if (chessMatch.getCheckMate()) {
            statusLabel.setText("XEQUE-MATE!");
        } else if (chessMatch.getCheck()) {
            statusLabel.setText("XEQUE!");
        } else {
            statusLabel.setText("Partida em andamento");
        }
    }

    private void updateMoveHistory() {
        movesList.getItems().setAll(historyFormatter.format(chessMatch.getChessLog().getMoves()));
        if (!movesList.getItems().isEmpty()) {
            movesList.scrollTo(movesList.getItems().size() - 1);
        }
    }

    // --- Helpers ---

    private void clearSelection() {
        sourcePosition = null;
        possibleMoves = null;
    }

    private boolean isAllyPiece(int row, int col) {
        ChessPiece c = chessMatch.getPieces()[row][col];
        ChessPiece s = chessMatch.getPieces()[sourcePosition.getRow()][sourcePosition.getColumn()];
        return c != null && s != null && c.getColor() == s.getColor();
    }

    private String getFriendlyPieceName(ChessPiece p) {
        return switch (p.getClass().getSimpleName()) {
            case "King" -> "Rei"; case "Queen" -> "Rainha"; case "Rook" -> "Torre";
            case "Bishop" -> "Bispo"; case "Knight" -> "Cavalo"; case "Pawn" -> "Peão";
            default -> p.getClass().getSimpleName();
        };
    }

    private String askPromotionPiece() {
        Map<String, String> map = Map.of("Rainha", "Q", "Torre", "R", "Bispo", "B", "Cavalo", "N");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Rainha", map.keySet());
        dialog.setTitle("Promoção");
        dialog.setHeaderText("Escolha a peça:");
        return dialog.showAndWait().map(map::get).orElse("Q");
    }

    private BotEngine createBotEngine(GameConfig config) {
        if (!config.getMode().hasAiPlayer()) return null;
        String path = config.getStockfishPath();
        return (path != null && !path.isBlank()) ? new StockfishEngine(path) : new StockfishEngine();
    }

    // --- Actions ---

    @FXML
    public void onUndoAction() {
        if (gameConfig != null && !gameConfig.isAllowUndo()) return;
        chessMatch.undoLastMove();
        clearSelection();
        drawBoard();
        updateUI();
        updateMoveHistory();
    }

    @FXML
    public void onRedoAction() {
        chessMatch.redoMove();
        clearSelection();
        drawBoard();
        updateUI();
        updateMoveHistory();
    }

    @FXML
    public void onBackToSetupAction() {
        try {
            shutdown();
            navigationService.navigateToSetup(boardGrid);
        } catch (IOException e) {
            statusLabel.setText("Erro ao voltar para a configuração.");
            LOG.severe("Erro ao carregar tela de configuração: " + e.getMessage());
        }
    }

    public void shutdown() {
        clockController.stop();
        if (gameManager != null && gameManager.getBotEngine() != null) {
            gameManager.getBotEngine().stop();
        }
    }
}