package chess.ai;

import chess.core.ChessPosition;

public final class UCIMapper {

    private UCIMapper() {}

    public static ChessPosition source(String move) {

        return parse(
                move.charAt(0),
                move.charAt(1)
        );
    }

    public static ChessPosition target(String move) {

        return parse(
                move.charAt(2),
                move.charAt(3)
        );
    }

    private static ChessPosition parse(
            char file,
            char rank
    ) {

        return new ChessPosition(
                file,
                Character.getNumericValue(rank)
        );
    }
}