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
import gui.ChessBoardView;
import gui.audio.SoundManager;
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
    @FXML private Label whiteTimeLabel;
    @FXML private Label blackTimeLabel;

    @FXML private GridPane boardGrid;
    @FXML private Label turnLabel;
    @FXML private Label statusLabel;
    @FXML private ListView<String> movesList;

    // --- Internal State ---
    private ChessMatch chessMatch;
    private ChessBoardView chessBoardView;
    private GameConfig gameConfig;
    private GameSession session;
    private GameManager gameManager;
    private javafx.animation.Timeline gameClock;
        private BotEngine botEngine;
        private boolean botUnavailable;

        private Position sourcePosition;
        private boolean[][] possibleMoves;

    // --- Lifecycle & Initialization ---

    @FXML
    public void initialize() {
        chessBoardView = new ChessBoardView();
    }

    /**
     * Ponto de entrada para configurar a partida vindo da tela de Setup.
     */

    private boolean isSoundEnabled() {
        return gameConfig != null
                && gameConfig.isSoundEnabled();
    }

    private void playMoveSound(boolean wasCapture, boolean wasPromotion) {
        if (!isSoundEnabled()) {
            return;
        }

        if (chessMatch.getCheckMate()) {
            SoundManager.playCheckmate();
        } else if (chessMatch.getCheck()) {
            SoundManager.playCheck();
        } else if (wasPromotion) {
            SoundManager.playPromote();
        } else if (wasCapture) {
            SoundManager.playCapture();
        } else {
            SoundManager.playMove();
        }
    }

    public void setup(GameConfig config) {
        this.gameConfig = config;

        // 1. Criar o motor (IA)
        BotEngine engine = createBotEngine(config);
        this.botEngine = engine;

        // 2. Criar o Manager (Ele já recebe o motor)
        this.gameManager = new GameManager(config, engine);

        // 3. Criar a Session
        this.session = new GameSession(gameManager, config);

        // 4. Sincronizar o ChessMatch (Essencial para a UI e o Bot verem o mesmo jogo)
        this.chessMatch = gameManager.getMatch();

        this.sourcePosition = null;
        this.possibleMoves = null;

        // 5. Iniciar lógica e Relógio
        this.session.start();
        startClockTask(); // Agora o tempo vai começar a contar

        // 6. Iniciar o processo externo do Stockfish
        if (engine != null) {
            try {
                engine.start(); // Inicia o .exe
                LOG.info("Stockfish pronto para uso.");
            } catch (IOException e) {
                botUnavailable = true;
                LOG.severe("Falha crítica no Stockfish: " + e.getMessage());
                statusLabel.setText("IA indisponível: " + e.getMessage());
            }
        }

        drawBoard(); // Garante que o tabuleiro apareça
        updateUI();
        updateMoveHistory();

        // 7. Verifica se o Bot começa jogando (ex: modo EvE ou Bot de Brancas)
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
        // 1. Verifica se o estado atual permite interação humana
        // Impede cliques se o jogo acabou, está pausado ou se o Bot está pensando
        if (session != null && !session.getCurrentState().canPlayerMove()) {
            statusLabel.setText(session.getCurrentState().getStatusMessage());
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

        if (piece == null) {
            return;
        }

        if (piece.getColor() != chessMatch.getCurrentPlayer()) {
            statusLabel.setText("Essa peça não é do jogador atual.");
            return;
        }

        sourcePosition = pos;
        possibleMoves = chessMatch.possibleMoves(ChessPosition.fromPosition(pos));
        statusLabel.setText("Selecionado: " + getFriendlyPieceName(piece) + " em " + ChessPosition.fromPosition(pos));
    }

    private void executeMove(Position targetPos) {
        ChessPosition source = ChessPosition.fromPosition(sourcePosition);
        ChessPosition target = ChessPosition.fromPosition(targetPos);

        boolean wasCapture = chessMatch.getPieces()[targetPos.getRow()][targetPos.getColumn()] != null;

        chessMatch.performChessMove(source, target);

        boolean wasPromotion = chessMatch.getPromoted() != null;

        if (wasPromotion) {
            handlePromotion();
        }

        playMoveSound(wasCapture, wasPromotion);

        session.updateClock();
        clearSelection();
        updateUI();

        // 3. Verifica se o jogo acabou ou se é a vez do Bot
        if (chessMatch.getCheckMate()) {
            session.setCurrentState(GameState.CHECKMATE);
            statusLabel.setText("Fim de jogo: Xeque-mate!");
        } else if (gameConfig.isBotTurn(chessMatch.getCurrentPlayer())) {
            // Altera o estado para bloquear a UI enquanto a IA processa
            session.setCurrentState(GameState.BOT_THINKING);
            statusLabel.setText("IA está calculando...");
            scheduleBotMove();
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

    // --- AI Logic (Stockfish) ---

    private BotEngine createBotEngine(GameConfig config) {
        // Se o modo for PvP, não precisamos de motor de IA
        if (!config.getMode().hasAiPlayer()) return null; // Retorna nulo se for PvP

        // Se o usuário especificou um caminho para o Stockfish, usamos ele.
        // Caso contrário, usamos o construtor padrão que busca no PATH do sistema.
        String path = config.getStockfishPath();

        if (path != null && !path.isBlank()) {
            return new StockfishEngine(path);
        } else {
            return new StockfishEngine();
        }
    }

    private void startBotEngine() {
        try {
            // gameManager.getBotEngine() retorna a instância criada no setup
            BotEngine bot = gameManager.getBotEngine();
            if (bot != null) {
                bot.start(); // Inicia o processo e o handshake UCI
                LOG.info("Stockfish iniciado com sucesso.");
            }
        } catch (IOException e) {
            LOG.severe("Erro ao iniciar Stockfish: " + e.getMessage());
            statusLabel.setText("Erro: IA não pôde ser iniciada.");
            // Opcional: Reverter para modo PvP ou exibir alerta
        }
    }

    private void scheduleBotMove() {
        if (botEngine == null) {
            statusLabel.setText("IA não configurada.");
            return;
        }

        if (!botEngine.isReady()) {
            statusLabel.setText("IA indisponível: Stockfish não está pronto.");
            session.setCurrentState(GameState.RUNNING);
            return;
        }

        // Roda em uma Thread separada para não travar o JavaFX
        new Thread(() -> {
            try {
                // Envia a posição inicial + todos os lances feitos até agora
                botEngine.setPosition("startpos", buildMoveHistory());

                // Pede o melhor lance para a dificuldade configurada
                Optional<String> uciMove = botEngine.getBestMove(
                        gameConfig.getDifficulty(),
                        chessMatch.getCurrentPlayer()
                );

                // Volta para a Thread da UI para aplicar o lance
                Platform.runLater(() -> {
                    uciMove.ifPresentOrElse(
                            this::applyBotMove,
                            () -> {
                                statusLabel.setText("Erro: IA não retornou um lance.");
                                session.setCurrentState(GameState.RUNNING);
                            }
                    );
                });
            } catch (Exception e) {
                LOG.severe("Falha na Thread do Bot: " + e.getMessage());
                Platform.runLater(() -> {
                    statusLabel.setText("Erro ao processar lance da IA.");
                    session.setCurrentState(GameState.RUNNING);
                });
            }
        }, "BotThread").start();
    }

    private void applyBotMove(String uciMove) {
        try {
            // Uso da sua nova classe utilitária
            ChessPosition source = UCIMapper.source(uciMove);
            ChessPosition target = UCIMapper.target(uciMove);

            Position targetPosition = target.toPosition();
            boolean wasCapture = chessMatch.getPieces()[targetPosition.getRow()][targetPosition.getColumn()] != null;

            // Execução direta no ChessMatch
            chessMatch.performChessMove(source, target);

            boolean wasPromotion = chessMatch.getPromoted() != null;

            // Lógica de promoção (se o quinto caractere existir, ex: "e7e8q")
            if (chessMatch.getPromoted() != null) {
                String piece = uciMove.length() == 5
                        ? String.valueOf(uciMove.charAt(4)).toUpperCase()
                        : "Q";
                chessMatch.replacePromotedPiece(piece);
            }

            playMoveSound(wasCapture, wasPromotion);

            if (chessMatch.getCheckMate()) {
                session.setCurrentState(GameState.CHECKMATE);
                statusLabel.setText("Fim de jogo: Xeque-mate!");
            } else {
                session.setCurrentState(chessMatch.getCheck() ? GameState.CHECK : GameState.RUNNING);
            }

            drawBoard();
            updateUI();
            updateMoveHistory();

            if (!chessMatch.getCheckMate() && gameConfig.isBotTurn(chessMatch.getCurrentPlayer())) {
                session.setCurrentState(GameState.BOT_THINKING);
                statusLabel.setText("IA está calculando...");
                scheduleBotMove();
            }
        } catch (Exception e) {
            LOG.warning("Erro ao mapear lance do Bot: " + e.getMessage());
            session.setCurrentState(GameState.RUNNING);
        }
    }

    // --- Timeline

    private void startClockTask() {
        // Timeline executa um bloco de código periodicamente
        gameClock = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                    // Só atualiza se a sessão existir e o jogo estiver rodando
                    if (session != null && session.getCurrentState() == GameState.RUNNING) {
                        session.updateClock();
                        updateTimerUI(); // Método para dar setText nas Labels
                    }
                })
        );
        gameClock.setCycleCount(javafx.animation.Animation.INDEFINITE);
        gameClock.play();
    }

    private void updateTimerUI() {
        whiteTimeLabel.setText(session.getFormattedWhiteTime());
        blackTimeLabel.setText(session.getFormattedBlackTime());

        if (session.getCurrentState() == GameState.TIMEOUT) {
            statusLabel.setText("FIM DE TEMPO!");
        }
    }

    private void updateTimerLabels() {
        // Exemplo: assumindo que você tenha labels para os tempos
        // whiteTimeLabel.setText(session.getFormattedWhiteTime());
        // blackTimeLabel.setText(session.getFormattedBlackTime());

        // Se o tempo acabar, a session muda o estado internamente e você reflete aqui
        if (session.getCurrentState() == GameState.TIMEOUT) {
            statusLabel.setText("FIM DE TEMPO!");
            drawBoard(); // Trava o tabuleiro
        }
    }

    // --- UI Helpers ---

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