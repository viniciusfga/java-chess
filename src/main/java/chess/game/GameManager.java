package chess.game;

import chess.ai.BotEngine;
import chess.ai.BotDifficulty;
import chess.ai.UCIMapper;
import chess.core.ChessMatch;
import chess.core.ChessPiece;
import chess.core.ChessPosition;
import chess.history.Move;
import java.util.stream.Collectors;

public class GameManager {

    private final ChessMatch chessMatch;
    private final GameConfig config;
    private final BotEngine botEngine;

    public GameManager(GameConfig config, BotEngine botEngine) {
        this.config = config;
        this.chessMatch = new ChessMatch();
        this.botEngine = botEngine;
    }

    /**
     * Executa um movimento humano e, se necessário, dispara a jogada do Bot.
     */
    public void performMove(ChessPosition source, ChessPosition target) {
        chessMatch.performChessMove(source, target);

        // Se após o lance humano for a vez do bot, processamos a IA
        if (shouldBotPlay()) {
            processBotTurn();
        }
    }

    /**
     * Transforma o histórico de movimentos do ChessMatch em uma string UCI
     * compatível com o Stockfish (ex: "e2e4 e7e5 g1f3").
     */
    public String buildMoveHistory() {
        return chessMatch.getChessLog().getMoves().stream()
                .map(Move::getUciNotation)
                .collect(Collectors.joining(" "));
    }

    private boolean shouldBotPlay() {
        return botEngine != null
                && !chessMatch.getCheckMate()
                && config.isBotTurn(chessMatch.getCurrentPlayer());
    }

    private void processBotTurn() {
        if (botEngine == null || !botEngine.isReady()) return;

        // 1. Sincroniza o motor com a posição atual do tabuleiro
        botEngine.setPosition("startpos", buildMoveHistory());

        // 2. Solicita o melhor lance
        botEngine.getBestMove(config.getDifficulty(), chessMatch.getCurrentPlayer())
                .ifPresent(uciMove -> {
                    // 3. Usa o seu UCIMapper para converter a string em posições do motor
                    ChessPosition src = UCIMapper.source(uciMove);
                    ChessPosition tgt = UCIMapper.target(uciMove);

                    chessMatch.performChessMove(src, tgt);

                    // Trata promoção do bot se houver (ex: e7e8q)
                    if (chessMatch.getPromoted() != null) {
                        String piece = uciMove.length() == 5
                                ? String.valueOf(uciMove.charAt(4)).toUpperCase()
                                : "Q";
                        chessMatch.replacePromotedPiece(piece);
                    }
                });
    }

    public boolean isCurrentPlayerWhite() {
        return chessMatch.getCurrentPlayer().isWhite();
    }


    // --- Getters para a UI ---
    public ChessPiece[][] getPieces() { return chessMatch.getPieces(); }
    public ChessMatch getMatch() { return chessMatch; }
    public BotEngine getBotEngine() { return botEngine; }
}