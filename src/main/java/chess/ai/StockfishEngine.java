package chess.ai;

import chess.core.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
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
    private final Random random = new Random();
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

        try {
            startReaderThread();
            performUciHandshake();
        } catch (IOException e) {
            stop();
            throw e;
        }
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
    public boolean isReady() {
        if (!running || !ready || process == null || !process.isAlive()) {
            return false;
        }

        return waitForReady();
    }

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

    private Optional<String> waitForHeuristicMove(long timeoutMs, BotDifficulty difficulty) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        List<CandidateMove> candidates = new ArrayList<>();
        Optional<String> bestMove = Optional.empty();

        try {
            while (System.currentTimeMillis() < deadline) {
                String line = outputQueue.poll(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);

                if (line == null) {
                    continue;
                }

                parseCandidateMove(line).ifPresent(candidate -> {
                    candidates.removeIf(existing -> existing.multiPv == candidate.multiPv);
                    candidates.add(candidate);
                });

                if (UCIProtocol.isBestMoveLine(line)) {
                    bestMove = UCIProtocol.parseBestMove(line);
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (bestMove.isEmpty()) {
            return Optional.empty();
        }

        if (candidates.isEmpty() || difficulty.getCandidateMoves() <= 1) {
            return bestMove;
        }

        return Optional.of(selectMoveByDifficulty(candidates, bestMove.get(), difficulty));
    }

    private Optional<CandidateMove> parseCandidateMove(String line) {
        if (line == null || !line.startsWith("info ") || !line.contains(" pv ")) {
            return Optional.empty();
        }

        String[] tokens = line.split("\\s+");

        int multiPv = 1;
        int score = 0;
        String move = null;

        for (int i = 0; i < tokens.length; i++) {
            if ("multipv".equals(tokens[i]) && i + 1 < tokens.length) {
                multiPv = parseIntOrDefault(tokens[i + 1], 1);
            }

            if ("score".equals(tokens[i]) && i + 2 < tokens.length) {
                score = parseScore(tokens[i + 1], tokens[i + 2]);
            }

            if ("pv".equals(tokens[i]) && i + 1 < tokens.length) {
                move = tokens[i + 1];
                break;
            }
        }

        if (move == null || move.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new CandidateMove(move, multiPv, score));
    }

    private int parseScore(String type, String value) {
        int parsed = parseIntOrDefault(value, 0);

        if ("mate".equals(type)) {
            return parsed > 0 ? 100_000 - parsed : -100_000 - parsed;
        }

        return parsed;
    }

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String selectMoveByDifficulty(List<CandidateMove> candidates, String stockfishBestMove, BotDifficulty difficulty) {
        candidates.sort(Comparator.comparingInt(CandidateMove::multiPv));

        if (random.nextDouble() > difficulty.getMistakeChance()) {
            return stockfishBestMove;
        }

        List<CandidateMove> weakerMoves = candidates.stream()
                .filter(candidate -> !candidate.move.equals(stockfishBestMove))
                .toList();

        if (weakerMoves.isEmpty()) {
            return stockfishBestMove;
        }

        int maxIndex = Math.min(weakerMoves.size(), Math.max(1, difficulty.getCandidateMoves() - 1));
        int selectedIndex = random.nextInt(maxIndex);

        return weakerMoves.get(selectedIndex).move;
    }

    private record CandidateMove(String move, int multiPv, int score) { }

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