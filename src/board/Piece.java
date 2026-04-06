package board;

public abstract class Piece {

    protected Position position;
    protected Board board;

    public Piece(Board board) {
        this.board = board;
        this.position = null;
    }

    public abstract boolean[][] possibleMoves();

    public boolean possibleMove(Position position) {
        return possibleMoves()[position.getRow()][position.getColumn()];
    }

    public boolean isThereAnyPossibleMove() {
        boolean[][] moves = possibleMoves();

        for (int i = 0; i < moves.length; i++) {
            for (int j = 0; j < moves[i].length; j++) {
                if (moves[i][j]) {
                    return true;
                }
            }
        }

        return false;
    }
}