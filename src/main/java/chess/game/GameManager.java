package chess.game;

import chess.ai.BotEngine;
import chess.ai.BotDifficulty;
import chess.ai.UCIMapper;
import chess.core.ChessMatch;
import chess.core.ChessPiece;
import chess.core.ChessPosition;
import chess.history.Move;
import chess.core.ChessPiece;
import chess.core.ChessPosition;

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
     * Executa um movimento na partida.
     * A orquestração de turnos de bot deve ser feita pela camada chamadora.
     */
    public void performMove(ChessPosition source, ChessPosition target) {
        chessMatch.performChessMove(source, target);
    }

    public boolean isCurrentPlayerWhite() {
        return chessMatch.getCurrentPlayer().isWhite();
    }

    // --- Getters para a UI ---
    public ChessPiece[][] getPieces() {
        return chessMatch.getPieces();
    }

    public ChessMatch getMatch() {
        return chessMatch;
    }

    public BotEngine getBotEngine() {
        return botEngine;
    }
}