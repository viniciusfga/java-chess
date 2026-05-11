package chess.service;

import chess.ai.BotEngine;
import chess.ai.BotDifficulty;
import chess.core.Color;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class BotMoveService {

    private final BotEngine engine;

    public BotMoveService(BotEngine engine) {
        this.engine = engine;
    }

    public CompletableFuture<Optional<String>> requestMove(
            String fen,
            String moves,
            BotDifficulty difficulty,
            Color color
    ) {

        return CompletableFuture.supplyAsync(() -> {
            engine.setPosition(fen, moves);
            return engine.getBestMove(difficulty, color);
        });
    }
}