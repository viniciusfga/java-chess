package chess.service;

import chess.core.ChessMatch;
import chess.history.Move;

public final class MoveHistorySerializer {

    private MoveHistorySerializer() {}

    public static String toUciMoves(
            ChessMatch match
    ) {

        return match.getChessLog()
                .getMoves()
                .stream()
                .map(Move::getUciNotation)
                .reduce(
                        "",
                        (a, b) -> a.isBlank()
                                ? b
                                : a + " " + b
                );
    }
}