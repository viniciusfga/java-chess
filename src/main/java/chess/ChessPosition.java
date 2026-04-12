package chess;

import board.Position;

public class ChessPosition {

    private char column;
    private int row;

    public ChessPosition(char column, int row) throws chess.ChessException {
        if (column < 'a' || column > 'h' || row < 1 || row > 8) {
            throw new chess.ChessException("Erro ao instanciar ChessPosition. Valores válidos são de a1 a h8.");
        }
        this.column = column;
        this.row = row;
    }

    public char getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    public Position toPosition() {
        return new Position(8 - row, column - 'a');
    }

    public static ChessPosition fromPosition(Position position) throws chess.ChessException {
        return new ChessPosition(
                (char) ('a' + position.getColumn()),
                8 - position.getRow()
        );
    }

    @Override
    public String toString() {
        return "" + column + row;
    }
}