package chess.history;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ChessLog {

    private List<Move> moves = new ArrayList<>();

    public void addMove(Move move) {
        moves.add(move);
    }

    public List<Move> getMoves() {
        return new ArrayList<>(moves);
    }

    public List<String> getFullHistory() {
        List<String> history = new ArrayList<>();

        for (int i = 0; i < moves.size(); i += 2) {
            int turn = (i / 2) + 1;
            String whiteMove = moves.get(i).getSanAnnotation();
            String blackMove = (i + 1 < moves.size()) ? moves.get(i + 1).getSanAnnotation() : "";
            history.add(turn + ". " + whiteMove + (blackMove.isBlank() ? "" : " " + blackMove));
        }

        return history;
    }

    public String toPgn(String event, String site, String white, String black, String result) {
        StringBuilder sb = new StringBuilder();

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));

        sb.append("[Event \"").append(event).append("\"]\n");
        sb.append("[Site \"").append(site).append("\"]\n");
        sb.append("[Date \"").append(date).append("\"]\n");
        sb.append("[White \"").append(white).append("\"]\n");
        sb.append("[Black \"").append(black).append("\"]\n");
        sb.append("[Result \"").append(result).append("\"]\n\n");

        for (int i = 0; i < moves.size(); i += 2) {
            int turn = (i / 2) + 1;
            String whiteMove = moves.get(i).getSanAnnotation();
            String blackMove = (i + 1 < moves.size()) ? moves.get(i + 1).getSanAnnotation() : "";
            sb.append(turn).append(". ").append(whiteMove);
            if (!blackMove.isBlank()) {
                sb.append(" ").append(blackMove);
            }
            sb.append(" ");
        }

        sb.append(result);
        return sb.toString().trim();
    }

    // Dentro da classe ChessLog
    public void removeLastMove() {
        if (!moves.isEmpty()) {
            moves.remove(moves.size() - 1);
        }
    }
}