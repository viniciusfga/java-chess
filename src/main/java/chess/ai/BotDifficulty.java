package chess.ai;

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
            800,
            0,
            500,
            5,
            -1
    ),

    // ── Nível 2 – Fácil ─────────────────────────────────────────────────────
    EASY(
            "Fácil",
            1100,
            5,
            1_000,
            8,
            -1
    ),

    // ── Nível 3 – Médio ─────────────────────────────────────────────────────
    MEDIUM(
            "Médio",
            1500,
            10,
            2_000,
            12,
            -1
    ),

    // ── Nível 4 – Difícil ───────────────────────────────────────────────────
    HARD(
            "Difícil",
            1900,
            15,
            3_000,
            16,
            -1
    ),

    // ── Nível 5 – Expert ────────────────────────────────────────────────────
    EXPERT(
            "Expert",
            2400,
            20,
            5_000,
            20,
            -1
    ),

    // ── Nível 6 – Máximo (Stockfish sem limitações) ─────────────────────────
    MAXIMUM(
            "Máximo",
            3190,   // teto do UCI_Elo do Stockfish 16+
            20,
            10_000,
            -1,
            -1
    );

    // ────────────────────────────────────────────────────────────────────────

    /** Rótulo exibido na interface. */
    private final String displayName;

    /** Rating UCI_Elo alvo. */
    private final int elo;

    /** Parâmetro "Skill Level" do Stockfish (0-20). */
    private final int skillLevel;

    /** Tempo máximo de busca por lance (ms). */
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

    // ────────────────────────────────────────────────────────────────────────

    BotDifficulty(String displayName, int elo, int skillLevel,
                  int moveTimeMs, int depth, long nodes) {
        this.displayName = displayName;
        this.elo         = elo;
        this.skillLevel  = skillLevel;
        this.moveTimeMs  = moveTimeMs;
        this.depth       = depth;
        this.nodes       = nodes;
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    public String getDisplayName() { return displayName; }
    public int    getElo()         { return elo; }
    public int    getSkillLevel()  { return skillLevel; }
    public int    getMoveTimeMs()  { return moveTimeMs; }
    public int    getDepth()       { return depth; }
    public long   getNodes()       { return nodes; }

    /** True quando o nível deve limitar o engine via UCI_Elo/UCI_LimitStrength. */
    public boolean usesEloLimit() {
        return this != MAXIMUM;
    }

    /**
     * Monta a parte do comando "go" com os parâmetros de busca.
     * Exemplo de saída: "go movetime 2000 depth 12"
     */
    public String buildGoCommand() {
        StringBuilder sb = new StringBuilder("go movetime ").append(moveTimeMs);
        if (depth > 0)  sb.append(" depth ").append(depth);
        if (nodes > 0)  sb.append(" nodes ").append(nodes);
        return sb.toString();
    }

    @Override
    public String toString() { return displayName; }
}