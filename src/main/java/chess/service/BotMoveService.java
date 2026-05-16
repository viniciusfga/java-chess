package chess.service;

import chess.ai.BotDifficulty;
import chess.ai.BotEngine;
import chess.core.Color;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Serviço responsável por solicitar movimentos do motor de IA de forma assíncrona.
 */
public class BotMoveService {

    private static final Logger LOG = Logger.getLogger(BotMoveService.class.getName());
    private static final int TIMEOUT_SECONDS = 30;

    private final BotEngine engine;
    private String lastPositionSet = null;

    public BotMoveService(BotEngine engine) {
        this.engine = engine;
    }

    /**
     * Verifica se o motor está pronto para processar movimentos.
     */
    public boolean isEngineReady() {
        return engine != null && engine.isReady();
    }

    /**
     * Solicita o melhor movimento do bot de forma assíncrona.
     *
     * @param moveHistory Histórico de movimentos em notação UCI (ex: "e2e4 e7e5")
     * @param difficulty Nível de dificuldade do bot
     * @param currentPlayer Cor do jogador atual
     * @return CompletableFuture com Optional<String> contendo o movimento UCI
     */
    public CompletableFuture<Optional<String>> requestMove(
            String moveHistory,
            BotDifficulty difficulty,
            Color currentPlayer
    ) {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        if (!isEngineReady()) {
                            LOG.warning("Motor de IA indisponível.");
                            return Optional.<String>empty();  // ✅ Cast explícito
                        }

                        String normalizedHistory = (moveHistory == null || moveHistory.isBlank())
                                ? ""
                                : moveHistory.trim();

                        // Otimização: só atualiza se a posição mudou
                        if (!normalizedHistory.equals(lastPositionSet)) {
                            engine.setPosition("startpos", normalizedHistory);
                            lastPositionSet = normalizedHistory;
                            LOG.fine("Posição atualizada no motor: " + normalizedHistory);
                        }

                        return engine.getBestMove(difficulty, currentPlayer);

                    } catch (Exception e) {
                        LOG.severe("Falha ao calcular movimento: " + e.getMessage());
                        e.printStackTrace();
                        return Optional.<String>empty();
                    }
                })
                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    if (ex instanceof java.util.concurrent.TimeoutException) {
                        LOG.severe("Timeout ao calcular movimento (>" + TIMEOUT_SECONDS + "s)");
                    } else {
                        LOG.severe("Erro no BotMoveService: " + ex.getMessage());
                    }
                    return Optional.<String>empty();
                });
    }

    /**
     * Limpa o cache de posição. Útil ao reiniciar uma partida.
     */
    public void resetCache() {
        this.lastPositionSet = null;
    }
}