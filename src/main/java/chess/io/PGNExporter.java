package chess.io;

import chess.core.ChessMatch;
import chess.history.Move;

import java.time.LocalDate;
import java.util.List;

public class PGNExporter {

    public static String export(ChessMatch match, String white, String black, String result) {
        StringBuilder sb = new StringBuilder();

        sb.append("[Event \"IF Goiano Chess\"]\n");
        sb.append("[Site \"Urutaí, Brazil\"]\n");
        sb.append("[Date \"").append(LocalDate.now()).append("\"]\n");
        sb.append("[White \"").append(white).append("\"]\n");
        sb.append("[Black \"").append(black).append("\"]\n");
        sb.append("[Result \"").append(result).append("\"]\n\n");

        List<Move> moves = match.getChessLog().getMoves();

        for (int i = 0; i < moves.size(); i += 2) {
            int turn = (i / 2) + 1;

            sb.append(turn).append(". ");
            sb.append(moves.get(i).getSanAnnotation()).append(" ");

            if (i + 1 < moves.size()) {
                sb.append(moves.get(i + 1).getSanAnnotation()).append(" ");
            }
        }

        sb.append(result);

        return sb.toString();
    }
}