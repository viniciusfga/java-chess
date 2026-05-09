package chess.engine;

import board.Board;
import board.Piece;
import chess.core.ChessMatch;
import chess.core.ChessPiece;
import chess.core.ChessPosition;
import chess.core.Color;
import chess.pieces.*;

import java.util.List;

public class BoardInitializer {

    private final Board board;
    private final List<Piece> piecesOnTheBoard;
    private final ChessMatch match;

    public BoardInitializer(Board board, List<Piece> piecesOnTheBoard, ChessMatch match) {
        this.board = board;
        this.piecesOnTheBoard = piecesOnTheBoard;
        this.match = match;
    }

    public void setup() {
        placeBackRank(Color.WHITE, 1);
        placePawnRank(Color.WHITE, 2);
        placeBackRank(Color.BLACK, 8);
        placePawnRank(Color.BLACK, 7);
    }

    private void placeBackRank(Color color, int row) {
        place('a', row, new Rook(board, color));
        place('b', row, new Knight(board, color));
        place('c', row, new Bishop(board, color));
        place('d', row, new Queen(board, color));
        place('e', row, new King(board, color, match));
        place('f', row, new Bishop(board, color));
        place('g', row, new Knight(board, color));
        place('h', row, new Rook(board, color));
    }

    private void placePawnRank(Color color, int row) {
        for (char col = 'a'; col <= 'h'; col++) {
            place(col, row, new Pawn(board, color, match));
        }
    }

    private void place(char column, int row, ChessPiece piece) {
        board.placePiece(piece, new ChessPosition(column, row).toPosition());
        piecesOnTheBoard.add(piece);
    }
}