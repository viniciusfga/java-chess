package chess.game;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Representa uma sessão ativa de jogo.
 * Gerencia o tempo decorrido, identificação única e o estado global da partida.
 */
public class GameSession {

    private final String sessionId;
    private final GameManager gameManager;
    private final GameConfig config;
    private final Instant startTime;

    private GameState currentState;
    private Duration whiteRemainingTime;
    private Duration blackRemainingTime;
    private Instant lastTurnSwitch;

    public GameSession(GameManager gameManager, GameConfig config) {
        this.sessionId = UUID.randomUUID().toString().substring(0, 8);
        this.gameManager = gameManager;
        this.config = config;
        this.startTime = Instant.now();
        this.currentState = GameState.CREATED;

        if (config.hasTimedGame()) {
            this.whiteRemainingTime = Duration.ofSeconds(config.getTotalTimeSeconds());
            this.blackRemainingTime = Duration.ofSeconds(config.getTotalTimeSeconds());
        } else {
            this.whiteRemainingTime = Duration.ZERO;
            this.blackRemainingTime = Duration.ZERO;
        }
    }

    /**
     * Inicia oficialmente a contagem de tempo e o fluxo de jogo.
     */
    public void start() {
        this.currentState = GameState.RUNNING;
        this.lastTurnSwitch = Instant.now();
    }

    /**
     * Atualiza o tempo do jogador atual e alterna o turno.
     */
    public void updateClock() {
        if (currentState != GameState.RUNNING) return;

        Instant now = Instant.now();
        Duration elapsed = Duration.between(lastTurnSwitch, now);

        if (gameManager.getMatch().getCurrentPlayer().isWhite()) {
            whiteRemainingTime = whiteRemainingTime.minus(elapsed);
        } else {
            blackRemainingTime = blackRemainingTime.minus(elapsed);
        }

        lastTurnSwitch = now;
        checkTimeOver();
    }

    private void checkTimeOver() {
        if (whiteRemainingTime.isNegative() || whiteRemainingTime.isZero()) {
            this.currentState = GameState.FINISHED;
            // Lógica de vitória por tempo para as pretas
        } else if (blackRemainingTime.isNegative() || blackRemainingTime.isZero()) {
            this.currentState = GameState.FINISHED;
            // Lógica de vitória por tempo para as brancas
        }
    }

    // --- Getters & Setters ---
    public GameConfig getConfig() {
        return config;
    }

    public String getSessionId() { return sessionId; }
    public GameManager getManager() { return gameManager; }
    public GameState getCurrentState() { return currentState; }
    public void setCurrentState(GameState state) { this.currentState = state; }

    public String getFormattedWhiteTime() { return formatDuration(whiteRemainingTime); }
    public String getFormattedBlackTime() { return formatDuration(blackRemainingTime); }

    private String formatDuration(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}