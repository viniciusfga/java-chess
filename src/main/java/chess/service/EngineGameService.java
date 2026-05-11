package chess.service;

import chess.ai.BotDifficulty;
import chess.ai.BotEngine;
import chess.ai.UCIMapper;
import chess.core.ChessMatch;
import chess.core.ChessPosition;

import java.util.Optional;

public class EngineGameService {

    private final BotEngine engine;

    private final ChessMatch match;

    public EngineGameService(
            BotEngine engine,
            ChessMatch match
    ) {

        this.engine = engine;
        this.match = match;
    }

    public void playBestMove(
            BotDifficulty difficulty
    ) {

        String moves =
                buildMoveHistory();

        engine.setPosition(
                "startpos",
                moves
        );

        Optional<String> bestMove =
                engine.getBestMove(
                        difficulty,
                        match.getCurrentPlayer()
                );

        if (bestMove.isEmpty()) {

            throw new IllegalStateException(
                    "Engine não retornou lance."
            );
        }

        applyUciMove(bestMove.get());
    }

    private void applyUciMove(
            String uciMove
    ) {

        ChessPosition src =
                UCIMapper.source(uciMove);

        ChessPosition tgt =
                UCIMapper.target(uciMove);

        match.performChessMove(src, tgt);

        if (uciMove.length() == 5 &&
                match.getPromoted() != null) {

            String promoted =
                    String.valueOf(
                            Character.toUpperCase(
                                    uciMove.charAt(4)
                            )
                    );

            match.replacePromotedPiece(
                    promoted
            );
        }
    }

    private String buildMoveHistory() {

        return match.getChessLog()
                .getMoves()
                .stream()
                .map(move -> move.getUciNotation())
                .reduce(
                        "",
                        (a, b) -> a.isBlank()
                                ? b
                                : a + " " + b
                );
    }
}