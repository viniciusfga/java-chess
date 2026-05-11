package chess.ai;

import chess.core.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação de {@link BotEngine} que gerencia um processo externo do Stockfish
 * usando o protocolo UCI.
 *
 * <h2>Ciclo de vida</h2>
 * <pre>
 *   StockfishEngine engine = new StockfishEngine("/usr/bin/stockfish");
 *   engine.start();                         // abre o processo
 *   engine.setPosition("startpos", "");     // informa a posição
 *   Optional&lt;String&gt; move = engine.getBestMove(BotDifficulty.MEDIUM, Color.WHITE);
 *   engine.stop();                          // encerra o processo
 * </pre>
 *
 * <h2>Thread-safety</h2>
 * Os métodos {@link #setPosition} e {@link #getBestMove} NÃO são thread-safe entre si;
 * todo acesso ao engine deve ser serializado pelo chamador (ex.: pelo {@code GameController}).
 */
public class StockfishEngine implements BotEngine {

    private static final Logger LOG = Logger.getLogger(StockfishEngine.class.getName());

    /** Tempo máximo (ms) para receber "readyok" após "isready". */
    private static final int READY_TIMEOUT_MS  = 5_000;

    /** Tempo máximo (ms) para receber "uciok" após "uci". */
    private static final int UCI_TIMEOUT_MS    = 3_000;

    /** Buffer de linhas lidas do processo em background. */
    private final BlockingQueue<String> outputQueue = new LinkedBlockingQueue<>();

    private final String executablePath;

    private Process        process;
    private PrintWriter    writer;
    private BufferedReader reader;
    private Thread         readerThread;

    private volatile boolean ready   = false;
    private volatile boolean running = false;

    // ── Construtores ─────────────────────────────────────────────────────────

    /**
     * Cria uma instância buscando o executável no PATH do sistema.
     * Equivalente a {@code new StockfishEngine("stockfish")}.
     */
    public StockfishEngine() {
        this("stockfish");
    }

    /**
     * Cria uma instância apontando para um executável específico.
     *
     * @param executablePath caminho absoluto ou nome no PATH (ex.: "stockfish")
     */
    public StockfishEngine(String executablePath) {
        this.executablePath = executablePath;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BotEngine — implementação
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public void start() throws IOException {
        validateExecutable();

        ProcessBuilder pb = new ProcessBuilder(executablePath);
        pb.redirectErrorStream(true);           // stderr → stdout
        process = pb.start();
        running = true;

        writer = new PrintWriter(process.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        startReaderThread();
        performUciHandshake();
    }

    @Override
    public void stop() {
        if (!running) return;
        running = false;
        ready   = false;

        sendCommand(UCIProtocol.quit());

        if (readerThread != null) {
            readerThread.interrupt();
        }
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }

        closeStreams();
        LOG.info("Stockfish encerrado.");
    }

    @Override
    public boolean isReady() {
        return running && ready;
    }

    @Override
    public void setPosition(String fen, String moves) {
        ensureRunning();
        sendCommand(UCIProtocol.position(fen, moves));
    }

    @Override
    public Optional<String> getBestMove(BotDifficulty difficulty, Color color) {
        ensureRunning();

        // Aplica as configurações de dificuldade
        for (String cmd : UCIProtocol.applyDifficulty(difficulty)) {
            sendCommand(cmd);
        }

        // Confirma que o engine está pronto antes de buscar
        if (!waitForReady()) {
            LOG.warning("Engine não respondeu isready antes da busca.");
            return Optional.empty();
        }

        // Inicia a busca
        outputQueue.clear();
        sendCommand(UCIProtocol.go(difficulty));

        // Aguarda "bestmove" com timeout = moveTime + margem de 2 s
        long timeout = difficulty.getMoveTimeMs() + 2_000L;
        return waitForBestMove(timeout);
    }

    @Override
    public void stopSearch() {
        if (running) sendCommand(UCIProtocol.stop());
    }

    @Override
    public String getName() {
        return "Stockfish";
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Métodos internos
    // ════════════════════════════════════════════════════════════════════════

    /** Verifica se o executável existe e é executável. */
    private void validateExecutable() throws IOException {
        // Se não for um caminho absoluto, confia no PATH
        if (!executablePath.contains("/") && !executablePath.contains("\\")) return;

        Path p = Path.of(executablePath);
        if (!Files.exists(p)) {
            throw new IOException("Executável do Stockfish não encontrado: " + executablePath);
        }
        if (!Files.isExecutable(p)) {
            throw new IOException("Sem permissão de execução: " + executablePath);
        }
    }

    /** Inicia a thread que lê as saídas do processo em background. */
    private void startReaderThread() {
        readerThread = new Thread(() -> {
            try {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    LOG.fine("← " + line);
                    outputQueue.put(line);
                }
            } catch (IOException e) {
                if (running) LOG.log(Level.WARNING, "Erro ao ler do Stockfish", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "stockfish-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * Faz o handshake inicial: envia "uci" e espera "uciok",
     * depois envia "isready" e espera "readyok".
     */
    private void performUciHandshake() throws IOException {
        sendCommand(UCIProtocol.uci());
        waitForLine("uciok", UCI_TIMEOUT_MS);
        sendCommand(UCIProtocol.uciNewGame());

        if (!waitForReady()) {
            throw new IOException("Stockfish não respondeu 'readyok' no tempo esperado.");
        }
        ready = true;
        LOG.info("Stockfish iniciado e pronto.");
    }

    /**
     * Envia "isready" e aguarda "readyok".
     *
     * @return true se recebeu "readyok" dentro do timeout
     */
    private boolean waitForReady() {
        sendCommand(UCIProtocol.isReady());
        try {
            waitForLine("readyok", READY_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            LOG.warning("Timeout esperando readyok: " + e.getMessage());
            return false;
        }
    }

    /**
     * Bloqueia até encontrar uma linha que contenha {@code token} ou até o timeout.
     *
     * @throws IOException se o timeout for atingido
     */
    private void waitForLine(String token, long timeoutMs) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        try {
            while (System.currentTimeMillis() < deadline) {
                long remaining = deadline - System.currentTimeMillis();
                String line = outputQueue.poll(remaining, TimeUnit.MILLISECONDS);
                if (line == null) break;
                if (line.contains(token)) return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        throw new IOException("Timeout aguardando '" + token + "' do Stockfish.");
    }

    /**
     * Aguarda uma linha {@code bestmove} do engine.
     *
     * @param timeoutMs tempo máximo de espera
     * @return lance em notação UCI, ou empty em caso de falha
     */
    private Optional<String> waitForBestMove(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        try {
            while (System.currentTimeMillis() < deadline) {
                long remaining = deadline - System.currentTimeMillis();
                String line = outputQueue.poll(remaining, TimeUnit.MILLISECONDS);
                if (line == null) break;

                if (UCIProtocol.isBestMoveLine(line)) {
                    Optional<String> move = UCIProtocol.parseBestMove(line);
                    move.ifPresent(m -> LOG.info("bestmove: " + m));
                    return move;
                }
                // Loga linhas de info para diagnóstico
                if (line.startsWith("info") && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("info depth=" + UCIProtocol.parseDepth(line)
                            + " score=" + UCIProtocol.parseScoreCp(line));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOG.warning("Timeout aguardando bestmove do Stockfish.");
        return Optional.empty();
    }

    /** Envia um comando ao processo do Stockfish. */
    private void sendCommand(String command) {
        if (writer != null) {
            LOG.fine("→ " + command);
            writer.println(command);
        }
    }

    private void ensureRunning() {
        if (!running) throw new IllegalStateException("StockfishEngine não está em execução. Chame start() primeiro.");
    }

    private void closeStreams() {
        try { if (writer != null) writer.close(); } catch (Exception ignored) {}
        try { if (reader != null) reader.close(); } catch (Exception ignored) {}
    }
}