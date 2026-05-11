package chess.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser e builder de mensagens do protocolo UCI (Universal Chess Interface).
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Montar comandos que são <em>enviados</em> ao engine (ex.: {@code setoption}, {@code go})</li>
 *   <li>Parsear respostas que chegam <em>do</em> engine (ex.: {@code bestmove}, {@code info})</li>
 * </ul>
 *
 * Esta classe é stateless; todos os métodos são estáticos.
 */
public final class UCIProtocol {

    // ── Padrões de resposta ──────────────────────────────────────────────────

    /** Captura o bestmove (e opcionalmente o ponder) de uma linha de resposta. */
    private static final Pattern BESTMOVE_PATTERN =
            Pattern.compile("^bestmove\\s+(\\S+)(?:\\s+ponder\\s+(\\S+))?");

    /** Captura o score em cp ou mate de uma linha de info. */
    private static final Pattern SCORE_PATTERN =
            Pattern.compile("score\\s+(cp|mate)\\s+(-?\\d+)");

    /** Captura a profundidade da linha de info. */
    private static final Pattern DEPTH_PATTERN =
            Pattern.compile("\\bdepth\\s+(\\d+)");

    /** Captura o pv (variante principal) de uma linha de info. */
    private static final Pattern PV_PATTERN =
            Pattern.compile("\\bpv\\s+(.+)$");

    // ── Construtor privado (utilitária) ──────────────────────────────────────

    private UCIProtocol() {}

    // ════════════════════════════════════════════════════════════════════════
    //  COMANDOS → ENGINE
    // ════════════════════════════════════════════════════════════════════════

    /** {@code uci} – inicia o handshake UCI. */
    public static String uci() {
        return "uci";
    }

    /** {@code isready} – verifica se o engine está pronto. */
    public static String isReady() {
        return "isready";
    }

    /** {@code ucinewgame} – sinaliza início de nova partida. */
    public static String uciNewGame() {
        return "ucinewgame";
    }

    /** {@code quit} – encerra o engine. */
    public static String quit() {
        return "quit";
    }

    /** {@code stop} – interrompe a busca em andamento. */
    public static String stop() {
        return "stop";
    }

    /**
     * Monta o comando {@code setoption} genérico.
     *
     * @param name  nome da opção UCI
     * @param value valor a ser atribuído
     */
    public static String setOption(String name, Object value) {
        return "setoption name " + name + " value " + value;
    }

    /**
     * Aplica todas as opções de dificuldade necessárias para um determinado nível.
     *
     * @param difficulty nível de dificuldade
     * @return lista ordenada de comandos {@code setoption} prontos para envio
     */
    public static List<String> applyDifficulty(BotDifficulty difficulty) {
        List<String> cmds = new ArrayList<>();

        if (difficulty.usesEloLimit()) {
            cmds.add(setOption("UCI_LimitStrength", "true"));
            cmds.add(setOption("UCI_Elo", difficulty.getElo()));
        } else {
            cmds.add(setOption("UCI_LimitStrength", "false"));
        }

        cmds.add(setOption("Skill Level", difficulty.getSkillLevel()));
        return Collections.unmodifiableList(cmds);
    }

    /**
     * Monta o comando {@code position} a partir de uma FEN e uma sequência de
     * movimentos UCI separados por espaço.
     *
     * @param fen   posição em FEN; use {@code "startpos"} para a posição inicial
     * @param moves movimentos em notação UCI, ex: "e2e4 e7e5" (pode ser vazio)
     */
    public static String position(String fen, String moves) {
        String base = "startpos".equalsIgnoreCase(fen)
                ? "position startpos"
                : "position fen " + fen;

        if (moves == null || moves.isBlank()) return base;
        return base + " moves " + moves.strip();
    }

    /**
     * Monta o comando {@code go} usando os parâmetros do nível de dificuldade.
     *
     * @param difficulty configurações de busca
     */
    public static String go(BotDifficulty difficulty) {
        return difficulty.buildGoCommand();
    }

    /**
     * Monta o comando {@code go} com tempo por jogador (modo relógio).
     *
     * @param whiteTimeMs  tempo restante das brancas em ms
     * @param blackTimeMs  tempo restante das pretas em ms
     * @param whiteIncMs   incremento por lance das brancas em ms
     * @param blackIncMs   incremento por lance das pretas em ms
     */
    public static String goWithClock(long whiteTimeMs, long blackTimeMs,
                                     long whiteIncMs, long blackIncMs) {
        return "go wtime " + whiteTimeMs
                + " btime " + blackTimeMs
                + " winc "  + whiteIncMs
                + " binc "  + blackIncMs;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RESPOSTAS ← ENGINE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Extrai o melhor movimento de uma linha {@code bestmove}.
     *
     * @param line linha bruta do engine
     * @return notação UCI do lance (ex.: "e2e4"), ou {@link Optional#empty()}
     */
    public static Optional<String> parseBestMove(String line) {
        if (line == null) return Optional.empty();
        Matcher m = BESTMOVE_PATTERN.matcher(line.strip());
        if (!m.find()) return Optional.empty();
        String move = m.group(1);
        // "bestmove (none)" indica que não há lance legal (xeque-mate / afogamento)
        return "(none)".equalsIgnoreCase(move) ? Optional.empty() : Optional.of(move);
    }

    /**
     * Extrai o movimento "ponder" de uma linha {@code bestmove}, se existir.
     *
     * @param line linha bruta do engine
     * @return lance para ponder em notação UCI, ou {@link Optional#empty()}
     */
    public static Optional<String> parsePonder(String line) {
        if (line == null) return Optional.empty();
        Matcher m = BESTMOVE_PATTERN.matcher(line.strip());
        if (!m.find()) return Optional.empty();
        String ponder = m.group(2);
        return (ponder != null && !"(none)".equalsIgnoreCase(ponder))
                ? Optional.of(ponder)
                : Optional.empty();
    }

    /**
     * Verifica se uma linha indica que o engine está pronto ({@code readyok}).
     */
    public static boolean isReadyOk(String line) {
        return line != null && line.strip().equals("readyok");
    }

    /**
     * Verifica se uma linha é a confirmação de protocolo UCI ({@code uciok}).
     */
    public static boolean isUciOk(String line) {
        return line != null && line.strip().equals("uciok");
    }

    /**
     * Verifica se a linha começa com {@code bestmove} (indica fim da busca).
     */
    public static boolean isBestMoveLine(String line) {
        return line != null && line.strip().startsWith("bestmove");
    }

    // ── Info parsing ─────────────────────────────────────────────────────────

    /**
     * Extrai a profundidade de uma linha {@code info depth …}.
     *
     * @return profundidade, ou -1 se não encontrada
     */
    public static int parseDepth(String line) {
        if (line == null) return -1;
        Matcher m = DEPTH_PATTERN.matcher(line);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /**
     * Extrai a avaliação da posição em centipawns de uma linha {@code info}.
     * Retorna {@link Integer#MIN_VALUE} quando o score é {@code mate}.
     *
     * @param line linha info do engine
     * @return centipawns (positivo = vantagem das brancas), ou MIN_VALUE para mate
     */
    public static int parseScoreCp(String line) {
        if (line == null) return 0;
        Matcher m = SCORE_PATTERN.matcher(line);
        if (!m.find()) return 0;
        if ("mate".equals(m.group(1))) return Integer.MIN_VALUE;
        return Integer.parseInt(m.group(2));
    }

    /**
     * Extrai o número de lances até o mate de uma linha {@code info}.
     * Retorna {@link Optional#empty()} se não for linha de mate.
     */
    public static Optional<Integer> parseMateIn(String line) {
        if (line == null) return Optional.empty();
        Matcher m = SCORE_PATTERN.matcher(line);
        if (!m.find() || !"mate".equals(m.group(1))) return Optional.empty();
        return Optional.of(Integer.parseInt(m.group(2)));
    }

    /**
     * Extrai a variante principal (pv) de uma linha {@code info}.
     *
     * @param line linha info do engine
     * @return lista de lances UCI, ou lista vazia
     */
    public static List<String> parsePV(String line) {
        if (line == null) return List.of();
        Matcher m = PV_PATTERN.matcher(line);
        if (!m.find()) return List.of();
        String[] moves = m.group(1).strip().split("\\s+");
        return List.of(moves);
    }

    // ── Validação básica de lance UCI ────────────────────────────────────────

    /**
     * Valida o formato básico de um lance UCI (ex.: "e2e4", "e7e8q").
     * Não verifica a legalidade do lance na posição atual.
     *
     * @param move lance em notação UCI
     * @return true se o formato for válido
     */
    public static boolean isValidUciMove(String move) {
        if (move == null) return false;
        // Formato: [a-h][1-8][a-h][1-8][qrbn]?
        return move.matches("[a-h][1-8][a-h][1-8][qrbn]?");
    }
}