package gui.service;

import chess.history.Move;
import java.util.ArrayList;
import java.util.List;

public class MoveHistoryFormatter {

    public List<String> format(List<Move> moves) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < moves.size(); i += 2) {
            String w = moves.get(i).getSanAnnotation();
            String b = (i + 1 < moves.size()) ? moves.get(i + 1).getSanAnnotation() : "";
            lines.add(String.format("%2d.  %-7s  %s", (i / 2) + 1, w, b));
        }
        return lines;
    }

    public String buildUciHistory(List<Move> moves) {
        return moves.stream()
                .map(Move::getUciNotation)
                .reduce("", (a, b) -> a.isBlank() ? b : a + " " + b);
    }
}