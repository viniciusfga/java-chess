package chess.ai;

import chess.core.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gerencia a comunicação via protocolo UCI com o executável do Stockfish.
 */
public class StockfishEngine implements BotEngine {

    private static final Logger LOG = Logger.getLogger(StockfishEngine.class.getName());
    private static final int READY_TIMEOUT_MS  = 5_000;
    private static final int UCI_TIMEOUT_MS    = 3_000;

    private final BlockingQueue<String> outputQueue = new LinkedBlockingQueue<>();
    private final String executablePath;

    private Process        process;
    private PrintWriter    writer;
    private BufferedReader reader;
    private Thread         readerThread;

    private volatile boolean ready   = false;
    private volatile boolean running = false;

    public StockfishEngine() { this("stockfish"); }

    public StockfishEngine(String executablePath) {
        this.executablePath = executablePath;
    }

    @Override
    public void start() throws IOException {
        validateExecutable();
        ProcessBuilder pb = new ProcessBuilder(executablePath);
        pb.redirectErrorStream(true);
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

        if (readerThread != null) readerThread.interrupt();
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        closeStreams();
    }

    @Override
    public boolean isReady() { return running && ready; }

    @Override
    public void setPosition(String fen, String moves) {
        ensureRunning();
        sendCommand(UCIProtocol.position(fen, moves));
    }

    @Override
    public Optional<String> getBestMove(BotDifficulty difficulty, Color color) {
        ensureRunning();
        for (String cmd : UCIProtocol.applyDifficulty(difficulty)) {
            sendCommand(cmd);
        }

        if (!waitForReady()) return Optional.empty();

        outputQueue.clear();
        sendCommand(UCIProtocol.go(difficulty));
        long timeout = difficulty.getMoveTimeMs() + 2_000L;
        return waitForBestMove(timeout);
    }

    @Override
    public void stopSearch() {
        if (running) sendCommand(UCIProtocol.stop());
    }

    @Override
    public String getName() { return "Stockfish"; }

    private void validateExecutable() throws IOException {
        if (!executablePath.contains("/") && !executablePath.contains("\\")) return;
        Path p = Path.of(executablePath);
        if (!Files.exists(p)) throw new IOException("Executável não encontrado: " + executablePath);
        if (!Files.isExecutable(p)) throw new IOException("Sem permissão de execução: " + executablePath);
    }

    private void startReaderThread() {
        readerThread = new Thread(() -> {
            try {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    outputQueue.put(line);
                }
            } catch (IOException | InterruptedException e) {
                if (running) LOG.log(Level.WARNING, "Erro no leitor do Stockfish", e);
            }
        }, "stockfish-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void performUciHandshake() throws IOException {
        sendCommand(UCIProtocol.uci());
        waitForLine("uciok", UCI_TIMEOUT_MS);
        sendCommand(UCIProtocol.uciNewGame());
        if (!waitForReady()) throw new IOException("Stockfish não respondeu readyok.");
        ready = true;
    }

    private boolean waitForReady() {
        sendCommand(UCIProtocol.isReady());
        try {
            waitForLine("readyok", READY_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void waitForLine(String token, long timeoutMs) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        try {
            while (System.currentTimeMillis() < deadline) {
                String line = outputQueue.poll(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                if (line != null && line.contains(token)) return;
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        throw new IOException("Timeout aguardando: " + token);
    }

    private Optional<String> waitForBestMove(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        try {
            while (System.currentTimeMillis() < deadline) {
                String line = outputQueue.poll(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                if (line != null && UCIProtocol.isBestMoveLine(line)) {
                    return UCIProtocol.parseBestMove(line);
                }
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return Optional.empty();
    }

    private void sendCommand(String command) {
        if (writer != null) writer.println(command);
    }

    private void ensureRunning() {
        if (!running || process == null || !process.isAlive()) {
            throw new IllegalStateException("Engine não está ativa.");
        }
    }

    private void closeStreams() {
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
        } catch (Exception ignored) { }
    }
}