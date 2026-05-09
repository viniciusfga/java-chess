package chess.io;

import chess.core.ChessMatch;
import chess.core.ChessPosition;
import chess.history.Move;
import console.UI;

import java.util.List;
import java.util.ArrayList;

public class ReplayManager {

    public static void replay(List<Move> moves) {
        ChessMatch replayMatch = new ChessMatch(); // ✅ corrigido

        for (Move move : moves) {
            System.out.println("Move: " + move.getSanAnnotation());

            replayMatch.performChessMove(
                    ChessPosition.fromPosition(move.getSource()),
                    ChessPosition.fromPosition(move.getTarget())
            );

            UI.printMatch(replayMatch, new ArrayList<>());
            pause();
        }
    }

    private static void pause() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
    }
}