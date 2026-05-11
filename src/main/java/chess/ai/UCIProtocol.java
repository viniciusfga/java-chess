package chess.ai;

import java.util.*;
import java.util.regex.*;

public final class UCIProtocol {
    private static final Pattern BESTMOVE_PATTERN = Pattern.compile("^bestmove\\s+(\\S+)(?:\\s+ponder\\s+(\\S+))?");
    private static final Pattern SCORE_PATTERN = Pattern.compile("score\\s+(cp|mate)\\s+(-?\\d+)");
    private static final Pattern DEPTH_PATTERN = Pattern.compile("\\bdepth\\s+(\\d+)");

    private UCIProtocol() {}

    public static String uci() { return "uci"; }
    public static String isReady() { return "isready"; }
    public static String uciNewGame() { return "ucinewgame"; }
    public static String quit() { return "quit"; }
    public static String stop() { return "stop"; }

    public static String setOption(String name, Object value) {
        return "setoption name " + name + " value " + value;
    }

    public static List<String> applyDifficulty(BotDifficulty diff) {
        List<String> cmds = new ArrayList<>();
        cmds.add(setOption("UCI_LimitStrength", diff.usesEloLimit()));
        if (diff.usesEloLimit()) cmds.add(setOption("UCI_Elo", diff.getElo()));
        cmds.add(setOption("Skill Level", diff.getSkillLevel()));
        return cmds;
    }

    public static String position(String fen, String moves) {
        String base = "startpos".equalsIgnoreCase(fen) ? "position startpos" : "position fen " + fen;
        return (moves == null || moves.isBlank()) ? base : base + " moves " + moves.strip();
    }

    public static String go(BotDifficulty diff) { return diff.buildGoCommand(); }

    public static Optional<String> parseBestMove(String line) {
        Matcher m = BESTMOVE_PATTERN.matcher(line.strip());
        if (!m.find()) return Optional.empty();
        String move = m.group(1);
        return "(none)".equalsIgnoreCase(move) ? Optional.empty() : Optional.of(move);
    }

    public static boolean isBestMoveLine(String line) {
        return line != null && line.strip().startsWith("bestmove");
    }

    public static int parseDepth(String line) {
        Matcher m = DEPTH_PATTERN.matcher(line);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    public static int parseScoreCp(String line) {
        Matcher m = SCORE_PATTERN.matcher(line);
        if (!m.find()) return 0;
        return "mate".equals(m.group(1)) ? Integer.MIN_VALUE : Integer.parseInt(m.group(2));
    }
}