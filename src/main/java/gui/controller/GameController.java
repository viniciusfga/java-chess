package gui.controller;

import board.Position;
import chess.ChessException;
import chess.ai.BotEngine;
import chess.ai.StockfishEngine;
import chess.core.ChessMatch;
import chess.core.ChessPiece;
import chess.core.ChessPosition;
import chess.game.GameConfig;
import chess.history.Move;
import gui.ChessBoardView;
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

/**
 * Controller principal da interface de jogo.
 * Gerencia a renderização do tabuleiro, interações do usuário e lances da IA.
 */
public class GameController {

    private static final Logger LOG = Logger.getLogger(GameController.class.getName());
    private static final String COORD_STYLE = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;";

    // --- FXML Elements ---
    @FXML private GridPane boardGrid;
    @FXML private Label turnLabel;
    @FXML private Label statusLabel;
    @FXML private ListView<String> movesList;

    // --- Internal State ---
    private ChessMatch chessMatch;
    private ChessBoardView chessBoardView;
    private GameConfig gameConfig;
    private BotEngine botEngine;

    private Position sourcePosition;
    private boolean[][] possibleMoves;

    // --- Lifecycle & Initialization ---

    @FXML
    public void initialize() {
        // Inicialização básica de componentes visuais
        chessBoardView = new ChessBoardView();

        // Se o setup() não for chamado externamente, inicia com config padrão
        if (gameConfig == null) {
            setup(GameConfig.defaultPvP());
        }
    }

    /**
     * Ponto de entrada para configurar a partida vindo da tela de Setup.
     */
    public void setup(GameConfig config) {
        this.gameConfig = config;
        this.chessMatch = new ChessMatch();
        this.sourcePosition = null;
        this.possibleMoves = null;

        if (gameConfig.getMode().hasAiPlayer()) {
            startBotEngine();
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

    // --- Board Rendering ---

    private void drawBoard() {
        boardGrid.getChildren().clear();
        ChessPiece[][] pieces = chessMatch.getPieces();

        // Renderiza as casas (Grid 1-8 para colunas de peças)
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
        // Ranks (1-8)
        for (int row = 0; row < 8; row++) {
            Label rank = new Label(String.valueOf(8 - row));
            rank.setStyle(COORD_STYLE);
            rank.setAlignment(Pos.CENTER_RIGHT);
            rank.setPadding(new Insets(0, 6, 0, 0));
            rank.setMaxHeight(Double.MAX_VALUE);
            boardGrid.add(rank, 0, row);
        }
        // Files (a-h)
        for (int col = 0; col < 8; col++) {
            Label file = new Label(String.valueOf((char) ('a' + col)));
            file.setStyle(COORD_STYLE);
            file.setAlignment(Pos.CENTER);
            file.setPadding(new Insets(4, 0, 0, 0));
            file.setMaxWidth(Double.MAX_VALUE);
            boardGrid.add(file, col + 1, 8);
        }
    }

    // --- Input Handling ---

    private void handleSquareClick(int row, int col) {
        // Bloqueia clique se for a vez do Bot
        if (gameConfig != null && gameConfig.isBotTurn(chessMatch.getCurrentPlayer())) {
            statusLabel.setText("Aguarde o lance da IA...");
            return;
        }

        Position clickedPos = new Position(row, col);

        try {
            if (sourcePosition == null) {
                handleSelection(clickedPos);
            } else if (sourcePosition.equals(clickedPos)) {
                clearSelection();
            } else if (isAllyPiece(row, col)) {
                handleSelection(clickedPos);
            } else {
                executeMove(clickedPos);
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
        if (piece != null) {
            sourcePosition = pos;
            possibleMoves = chessMatch.possibleMoves(ChessPosition.fromPosition(pos));
            statusLabel.setText("Selecionado: " + getFriendlyPieceName(piece) + " em " + ChessPosition.fromPosition(pos));
        }
    }

    private void executeMove(Position targetPos) {
        ChessPosition source = ChessPosition.fromPosition(sourcePosition);
        ChessPosition target = ChessPosition.fromPosition(targetPos);

        chessMatch.performChessMove(source, target);

        if (chessMatch.getPromoted() != null) {
            handlePromotion();
        }

        clearSelection();
        updateUI();

        if (!chessMatch.getCheckMate() && gameConfig.isBotTurn(chessMatch.getCurrentPlayer())) {
            scheduleBotMove();
        }
    }

    private void handlePromotion() {
        String choice = askPromotionPiece();
        chessMatch.replacePromotedPiece(choice);
        List<Move> moves = chessMatch.getChessLog().getMoves();
        Move lastMove = moves.get(moves.size() - 1);
        lastMove.setSanAnnotation(lastMove.getSanAnnotation() + "=" + choice);
    }

    // --- AI Logic (Stockfish) ---

    private void startBotEngine() {
        String path = gameConfig.getStockfishPath();
        botEngine = (path != null) ? new StockfishEngine(path) : new StockfishEngine();
        try {
            botEngine.start();
            LOG.info("BotEngine iniciado com sucesso.");
        } catch (IOException e) {
            LOG.severe("Falha ao iniciar motor: " + e.getMessage());
            statusLabel.setText("IA indisponível.");
        }
    }

    private void scheduleBotMove() {
        if (botEngine == null || !botEngine.isReady()) return;

        new Thread(() -> {
            botEngine.setPosition("startpos", buildMoveHistory());
            Optional<String> uciMove = botEngine.getBestMove(gameConfig.getDifficulty(), chessMatch.getCurrentPlayer());

            Platform.runLater(() -> uciMove.ifPresentOrElse(this::applyBotMove,
                    () -> statusLabel.setText("Bot falhou em calcular.")));
        }, "BotThread").start();
    }

    private void applyBotMove(String uciMove) {
        try {
            // Conversão UCI (ex: e2e4) para coordenadas de matriz
            int fC = uciMove.charAt(0) - 'a';
            int fR = 8 - Character.getNumericValue(uciMove.charAt(1));
            int tC = uciMove.charAt(2) - 'a';
            int tR = 8 - Character.getNumericValue(uciMove.charAt(3));

            chessMatch.performChessMove(ChessPosition.fromPosition(new Position(fR, fC)),
                    ChessPosition.fromPosition(new Position(tR, tC)));

            if (chessMatch.getPromoted() != null) {
                String p = uciMove.length() == 5 ? String.valueOf(uciMove.charAt(4)).toUpperCase() : "Q";
                chessMatch.replacePromotedPiece(p);
            }

            drawBoard();
            updateUI();
            updateMoveHistory();

            if (!chessMatch.getCheckMate() && gameConfig.isBotTurn(chessMatch.getCurrentPlayer())) {
                scheduleBotMove();
            }
        } catch (Exception e) {
            LOG.warning("Erro ao aplicar lance do Bot: " + e.getMessage());
        }
    }

    // --- UI Helpers ---

    private void updateUI() {
        turnLabel.setText(chessMatch.getCurrentPlayer().toString());
        if (chessMatch.getCheckMate()) statusLabel.setText("XEQUE-MATE!");
        else if (chessMatch.getCheck()) statusLabel.setText("XEQUE!");
        else statusLabel.setText("Partida em andamento");
    }

    private void updateMoveHistory() {
        movesList.getItems().clear();
        List<Move> moves = chessMatch.getChessLog().getMoves();
        for (int i = 0; i < moves.size(); i += 2) {
            String w = moves.get(i).getSanAnnotation();
            String b = (i + 1 < moves.size()) ? moves.get(i + 1).getSanAnnotation() : "";
            movesList.getItems().add(String.format("%2d.  %-7s  %s", (i / 2) + 1, w, b));
        }
        if (!movesList.getItems().isEmpty()) movesList.scrollTo(movesList.getItems().size() - 1);
    }

    private String buildMoveHistory() {
        return chessMatch.getChessLog().getMoves().stream()
                .map(Move::getUciNotation)
                .reduce("", (a, b) -> a.isBlank() ? b : a + " " + b);
    }

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

    // --- Actions ---

    @FXML public void onUndoAction() {
        if (gameConfig != null && !gameConfig.isAllowUndo()) return;
        chessMatch.undoLastMove();
        clearSelection();
        drawBoard();
        updateUI();
        updateMoveHistory();
    }

    @FXML public void onRedoAction() {
        chessMatch.redoMove();
        clearSelection();
        drawBoard();
        updateUI();
        updateMoveHistory();
    }

    public void shutdown() {
        if (botEngine != null) botEngine.stop();
    }
}