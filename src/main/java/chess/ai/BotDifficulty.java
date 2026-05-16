package chess.ai;

import org.jetbrains.annotations.NotNull;

/**
 * Mapeamento de níveis de dificuldade para parâmetros UCI do Stockfish.
 *
 * <p>Cada nível define:
 * <ul>
 *   <li>{@code elo}        – rating alvo enviado via {@code setoption name UCI_Elo}</li>
 *   <li>{@code skillLevel} – parâmetro "Skill Level" (0–20) do Stockfish</li>
 *   <li>{@code moveTime}   – tempo máximo de busca em ms (movetime)</li>
 *   <li>{@code depth}      – profundidade máxima de busca (-1 = sem limite)</li>
 *   <li>{@code nodes}      – nós máximos de busca (-1 = sem limite)</li>
 * </ul>
 *
 * UCI_LimitStrength deve ser habilitado junto com UCI_Elo para que o Stockfish
 * realmente jogue no rating desejado.
 */
public enum BotDifficulty {

    // ── Nível 1 – Iniciante ─────────────────────────────────────────────────
    BEGINNER(
            "Iniciante",
            400,
            0,
            100,
            1,
            50,
            5,
            0.70
    ),

    // ── Nível 2 – Fácil ─────────────────────────────────────────────────────
    EASY(
            "Fácil",
            1100,
            5,
            1_000,
            8,
            -1,
            4,
            0.45
    ),

    // ── Nível 3 – Médio ─────────────────────────────────────────────────────
    MEDIUM(
            "Médio",
            1500,
            10,
            2_000,
            12,
            -1,
            3,
            0.20
    ),

    // ── Nível 4 – Difícil ───────────────────────────────────────────────────
    HARD(
            "Difícil",
            1900,
            15,
            3_000,
            16,
            -1,
            2,
            0.08
    ),

    // ── Nível 5 – Expert ────────────────────────────────────────────────────
    EXPERT(
            "Expert",
            2400,
            20,
            5_000,
            20,
            -1,
            1,
            0.0
    ),

    // ── Nível 6 – Máximo (Stockfish sem limitações) ─────────────────────────
    MAXIMUM(
            "Máximo",
            3190,   // teto do UCI_Elo do Stockfish 16+
            20,
            10_000,
            -1,
            -1,
            1,
            0.0
    );

    // ────────────────────────────────────────────────────────────────────────

    /**
     * Rótulo exibido na interface.
     */
    private final String displayName;

    /**
     * Rating UCI_Elo alvo.
     */
    private final int elo;

    /**
     * Parâmetro "Skill Level" do Stockfish (0-20).
     */
    private final int skillLevel;

    /**
     * Tempo máximo de busca por lance (ms).
     */
    private final int moveTimeMs;

    /**
     * Profundidade máxima de busca.
     * -1 significa sem restrição de profundidade (o moveTime prevalece).
     */
    private final int depth;

    /**
     * Número máximo de nós a avaliar.
     * -1 significa sem restrição de nós.
     */
    private final long nodes;

    /**
     * Quantidade de lances candidatos analisados via MultiPV.
     */
    private final int candidateMoves;

    /**
     * Chance de escolher um lance inferior ao melhor. Valor entre 0.0 e 1.0.
     */
    private final double mistakeChance;

    // ────────────────────────────────────────────────────────────────────────

    BotDifficulty(String displayName, int elo, int skillLevel,
                  int moveTimeMs, int depth, long nodes,
                  int candidateMoves, double mistakeChance) {
        this.displayName = displayName;
        this.elo = elo;
        this.skillLevel = skillLevel;
        this.moveTimeMs = moveTimeMs;
        this.depth = depth;
        this.nodes = nodes;
        this.candidateMoves = candidateMoves;
        this.mistakeChance = mistakeChance;
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    public String getDisplayName() {
        return displayName;
    }

    public int getElo() {
        return elo;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public int getMoveTimeMs() {
        return moveTimeMs;
    }

    public int getDepth() {
        return depth;
    }

    public long getNodes() {
        return nodes;
    }

    public int getCandidateMoves() {
        return candidateMoves;
    }

    public double getMistakeChance() {
        return mistakeChance;
    }

    /**
     * Indica se o Stockfish deve limitar força por Elo.
     * No modo máximo, o engine joga sem limitação artificial.
     */
    public boolean usesEloLimit() {
        return this != MAXIMUM;
    }

    /**
     * Monta o comando UCI "go" conforme os limites configurados.
     */
    public @NotNull String buildGoCommand() {
        StringBuilder command = new StringBuilder("go");
        if (moveTimeMs > 0) {
            command.append(" movetime ").append(moveTimeMs);
        }
        if (depth > 0) {
            command.append(" depth ").append(depth);
        }
        if (nodes > 0) {
            command.append(" nodes ").append(nodes);
        }
        return command.toString();
    }
}