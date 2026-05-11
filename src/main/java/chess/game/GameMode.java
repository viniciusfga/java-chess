package chess.game;

/**
 * Modos de jogo suportados pela aplicação.
 *
 * <ul>
 *   <li>{@link #PVP}  – Jogador vs Jogador (dois humanos na mesma máquina)</li>
 *   <li>{@link #PVE}  – Jogador vs Engine  (humano contra a IA)</li>
 *   <li>{@link #EVE}  – Engine vs Engine   (demonstração automática)</li>
 * </ul>
 */
public enum GameMode {

    // ── Modos ────────────────────────────────────────────────────────────────

    /** Dois jogadores humanos, sem IA. */
    PVP("Jogador vs Jogador") {
        @Override public boolean hasHumanPlayer()  { return true;  }
        @Override public boolean hasAiPlayer()     { return false; }
        @Override public boolean needsBotForWhite() { return false; }
        @Override public boolean needsBotForBlack() { return false; }
    },

    /** Um humano (brancas por padrão) contra a IA (pretas por padrão). */
    PVE("Jogador vs IA") {
        @Override public boolean hasHumanPlayer()  { return true;  }
        @Override public boolean hasAiPlayer()     { return true;  }
        @Override public boolean needsBotForWhite() { return false; }
        @Override public boolean needsBotForBlack() { return true;  }
    },

    /** Dois bots jogam entre si; nenhuma interação humana necessária. */
    EVE("IA vs IA") {
        @Override public boolean hasHumanPlayer()  { return false; }
        @Override public boolean hasAiPlayer()     { return true;  }
        @Override public boolean needsBotForWhite() { return true;  }
        @Override public boolean needsBotForBlack() { return true;  }
    };

    // ── Estado ───────────────────────────────────────────────────────────────

    private final String displayName;

    GameMode(String displayName) {
        this.displayName = displayName;
    }

    // ── API pública ──────────────────────────────────────────────────────────

    /** Rótulo exibido na interface. */
    public String getDisplayName() { return displayName; }

    /** True se ao menos um dos lados é controlado por um humano. */
    public abstract boolean hasHumanPlayer();

    /** True se ao menos um dos lados é controlado por uma IA. */
    public abstract boolean hasAiPlayer();

    /**
     * True se o lado das brancas deve ser controlado por um bot.
     * Útil para o {@code GameController} decidir se deve chamar o engine
     * no turno das brancas.
     */
    public abstract boolean needsBotForWhite();

    /**
     * True se o lado das pretas deve ser controlado por um bot.
     * Útil para o {@code GameController} decidir se deve chamar o engine
     * no turno das pretas.
     */
    public abstract boolean needsBotForBlack();

    @Override
    public String toString() { return displayName; }
}