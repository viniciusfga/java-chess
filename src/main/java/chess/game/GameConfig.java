package chess.game;

import chess.ai.BotDifficulty;
import chess.core.Color;

import java.util.Objects;

/**
 * Imutável. Contém todas as configurações escolhidas na tela de setup antes
 * de uma partida começar. Criada via {@link Builder}.
 *
 * <h2>Exemplo de uso</h2>
 * <pre>{@code
 * GameConfig config = new GameConfig.Builder()
 *         .mode(GameMode.PVE)
 *         .humanColor(Color.WHITE)
 *         .difficulty(BotDifficulty.MEDIUM)
 *         .stockfishPath("/usr/bin/stockfish")
 *         .totalTimeSeconds(600)     // 10 min por lado
 *         .incrementSeconds(5)
 *         .allowUndo(true)
 *         .build();
 * }</pre>
 */
public final class GameConfig {

    // ── Campos ───────────────────────────────────────────────────────────────

    /** Modo da partida (PvP, PvE, EvE). */
    private final GameMode mode;

    /**
     * Cor controlada pelo jogador humano.
     * {@code null} em modo EVE (nenhum humano).
     * Em PvP, as brancas são consideradas o "jogador 1".
     */
    private final Color humanColor;

    /**
     * Dificuldade da IA.
     * Ignorado em PvP; obrigatório em PvE e EVE.
     */
    private final BotDifficulty difficulty;

    /**
     * Caminho para o executável do Stockfish.
     * Se {@code null}, o engine tentará achar "stockfish" no PATH do sistema.
     */
    private final String stockfishPath;

    /**
     * Tempo total por jogador em segundos.
     * 0 = sem relógio (partida livre).
     */
    private final int totalTimeSeconds;
    private final int timeControlMinutes;

    /**
     * Incremento por lance em segundos (Fischer).
     * 0 = sem incremento.
     */
    private final int incrementSeconds;

    /** Permite desfazer lances durante a partida. */
    private final boolean allowUndo;

    /** Habilita exibição de movimentos legais ao clicar em uma peça. */
    private final boolean showLegalMoves;

    /** Habilita som de tabuleiro (captura, xeque, etc.). */
    private final boolean soundEnabled;

    // ── Construtor privado (use o Builder) ───────────────────────────────────

    private GameConfig(Builder b) {
        this.mode             = b.mode;
        this.humanColor       = b.humanColor;
        this.difficulty       = b.difficulty;
        this.stockfishPath    = b.stockfishPath;
        this.totalTimeSeconds = b.totalTimeSeconds;
        this.incrementSeconds = b.incrementSeconds;
        this.allowUndo        = b.allowUndo;
        this.showLegalMoves   = b.showLegalMoves;
        this.soundEnabled     = b.soundEnabled;

        this.timeControlMinutes = (b.totalTimeSeconds > 0) ? (b.totalTimeSeconds / 60) : b.timeControlMinutes;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public GameMode      getMode()             { return mode;             }
    public Color         getHumanColor()       { return humanColor;       }
    public BotDifficulty getDifficulty()       { return difficulty;       }
    public String        getStockfishPath()    { return stockfishPath;    }
    public int           getTotalTimeSeconds() { return totalTimeSeconds; }
    public int           getIncrementSeconds() { return incrementSeconds; }
    public boolean       isAllowUndo()         { return allowUndo;        }
    public boolean       isShowLegalMoves()    { return showLegalMoves;   }
    public boolean       isSoundEnabled()      { return soundEnabled;     }
    public int getTimeControlMinutes() {
        return timeControlMinutes;
    }


    // ── Conveniências ────────────────────────────────────────────────────────

    /** True se a partida tiver controle de tempo configurado. */
    public boolean hasTimedGame() {
        return totalTimeSeconds > 0;
    }

    /**
     * Retorna a cor controlada pelo bot em modo PvE.
     * Em PvP retorna {@code null}. Em EVE, retorna {@link Color#WHITE} por convenção
     * (ambos são bots; use {@link GameMode#needsBotForWhite()} e {@link GameMode#needsBotForBlack()}).
     */
    public Color getBotColor() {
        if (mode == GameMode.PVP) return null;
        if (mode == GameMode.EVE) return Color.WHITE;
        return humanColor == Color.WHITE ? Color.BLACK : Color.WHITE;
    }

    /**
     * Determina se o engine deve jogar no turno da cor informada.
     *
     * @param turn cor do turno atual
     * @return true se o engine deve fazer o lance
     */
    public boolean isBotTurn(Color turn) {
        return switch (mode) {
            case PVP -> false;
            case PVE -> turn != humanColor;
            case EVE -> true;
        };
    }

    // ── equals / hashCode / toString ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameConfig gc)) return false;
        return totalTimeSeconds == gc.totalTimeSeconds
                && incrementSeconds == gc.incrementSeconds
                && allowUndo        == gc.allowUndo
                && showLegalMoves   == gc.showLegalMoves
                && soundEnabled     == gc.soundEnabled
                && mode             == gc.mode
                && humanColor       == gc.humanColor
                && difficulty       == gc.difficulty
                && Objects.equals(stockfishPath, gc.stockfishPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, humanColor, difficulty, stockfishPath,
                totalTimeSeconds, incrementSeconds, allowUndo, showLegalMoves, soundEnabled);
    }

    @Override
    public String toString() {
        return "GameConfig{mode=" + mode
                + ", humanColor=" + humanColor
                + ", difficulty=" + difficulty
                + ", time=" + totalTimeSeconds + "+" + incrementSeconds
                + ", allowUndo=" + allowUndo
                + '}';
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Builder
    // ════════════════════════════════════════════════════════════════════════

    public static final class Builder {

        // Valores padrão razoáveis
        private GameMode      mode             = GameMode.PVP;
        private Color         humanColor       = Color.WHITE;
        private BotDifficulty difficulty       = BotDifficulty.MEDIUM;
        private String        stockfishPath    = "engines/stockfish.exe";
        private int           totalTimeSeconds = 0;      // sem relógio
        private int           timeControlMinutes = 10;
        private int           incrementSeconds = 0;
        private boolean       allowUndo        = true;
        private boolean       showLegalMoves   = true;
        private boolean       soundEnabled     = true;

        public Builder() {}

        /** Copia as configurações de um {@link GameConfig} existente. */
        public Builder(GameConfig source) {
            this.mode             = source.mode;
            this.humanColor       = source.humanColor;
            this.difficulty       = source.difficulty;
            this.stockfishPath    = source.stockfishPath;
            this.totalTimeSeconds = source.totalTimeSeconds;
            this.incrementSeconds = source.incrementSeconds;
            this.allowUndo        = source.allowUndo;
            this.showLegalMoves   = source.showLegalMoves;
            this.soundEnabled     = source.soundEnabled;
        }

        public Builder mode(GameMode mode) {
            this.mode = Objects.requireNonNull(mode, "mode não pode ser nulo");
            return this;
        }

        public Builder humanColor(Color humanColor) {
            this.humanColor = humanColor;
            return this;
        }

        public Builder difficulty(BotDifficulty difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        public Builder stockfishPath(String path) {
            this.stockfishPath = path;
            return this;
        }

        /** Define tempo total por lado em segundos (0 = sem relógio). */
        public Builder totalTimeSeconds(int seconds) {
            if (seconds < 0) throw new IllegalArgumentException("Tempo não pode ser negativo.");
            this.totalTimeSeconds = seconds;
            return this;
        }

        /** Define incremento por lance em segundos (Fischer). */
        public Builder incrementSeconds(int seconds) {
            if (seconds < 0) throw new IllegalArgumentException("Incremento não pode ser negativo.");
            this.incrementSeconds = seconds;
            return this;
        }

        public Builder allowUndo(boolean allow) {
            this.allowUndo = allow;
            return this;
        }

        public Builder showLegalMoves(boolean show) {
            this.showLegalMoves = show;
            return this;
        }

        public Builder soundEnabled(boolean enabled) {
            this.soundEnabled = enabled;
            return this;
        }

        public Builder timeControlMinutes(int minutes) {
            this.timeControlMinutes = minutes;
            this.totalTimeSeconds = minutes * 60; // Sincroniza segundos
            return this;
        }

        /**
         * Valida e constrói o {@link GameConfig}.
         *
         * @throws IllegalStateException se a combinação de opções for inválida
         */
        public GameConfig build() {
            validate();
            return new GameConfig(this);
        }

        private void validate() {
            if (mode.hasAiPlayer() && difficulty == null) {
                throw new IllegalStateException("Um nível de dificuldade deve ser definido para modos com IA.");
            }
            if (mode == GameMode.PVE && humanColor == null) {
                throw new IllegalStateException("A cor do jogador humano deve ser definida no modo PvE.");
            }
        }
    }

    // ── Factory methods ──────────────────────────────────────────────────────

    /** Cria uma configuração PvP padrão (sem relógio, sem IA). */
    public static GameConfig defaultPvP() {
        return new Builder().mode(GameMode.PVP).build();
    }

    /** Cria uma configuração PvE com a dificuldade e cor informadas. */
    public static GameConfig pvE(Color humanColor, BotDifficulty difficulty) {
        return new Builder()
                .mode(GameMode.PVE)
                .humanColor(humanColor)
                .difficulty(difficulty)
                .build();
    }

    /** Cria uma configuração EvE com a dificuldade informada. */
    public static GameConfig evE(BotDifficulty difficulty) {
        return new Builder()
                .mode(GameMode.EVE)
                .difficulty(difficulty)
                .build();
    }
}