package chess.game;

import chess.ai.BotEngine;
import chess.ai.StockfishEngine;
import chess.core.ChessMatch;
import chess.core.ChessPiece;
import chess.core.ChessPosition;

public class GameManager {

    private final ChessMatch chessMatch;
    private final GameConfig config;
    private final BotEngine botEngine;

    public GameManager(GameConfig config) {
        this.config = config;
        this.chessMatch = new ChessMatch();

        if (config.getMode().hasAiPlayer()) {
            this.botEngine = new StockfishEngine();
        } else {
            this.botEngine = null;
        }
    }

    public void performMove(
            ChessPosition source,
            ChessPosition target
    ) {
        chessMatch.performChessMove(source, target);

        if (shouldBotPlay()) {
            performBotMove();
        }
    }

    private boolean shouldBotPlay() {

        if (botEngine == null) {
            return false;
        }

        return config.isBotTurn(
                chessMatch.getCurrentPlayer()
        );
    }

    private void performBotMove() {

        // implementação futura
        System.out.println("Bot jogando...");
    }

    public ChessPiece[][] getPieces() {
        return chessMatch.getPieces();
    }

    public boolean[][] possibleMoves(ChessPosition pos) {
        return chessMatch.possibleMoves(pos);
    }

    public void undo() {
        chessMatch.undoLastMove();
    }

    public void redo() {
        chessMatch.redoMove();
    }
}