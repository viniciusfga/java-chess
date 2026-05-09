package chess.engine;

import board.Board;
import board.Piece;
import board.Position;
import chess.core.ChessPiece;
import chess.core.Color;
import chess.pieces.King;

import java.util.List;
import java.util.stream.Collectors;

public class GameStateEvaluator {

    private final Board board;
    private final List<Piece> piecesOnTheBoard;
    private final MoveExecutor executor;

    public GameStateEvaluator(Board board, List<Piece> piecesOnTheBoard, MoveExecutor executor) {
        this.board = board;
        this.piecesOnTheBoard = piecesOnTheBoard;
        this.executor = executor;
    }

    public ChessPiece king(Color color) {
        return piecesOnTheBoard.stream()
                .map(p -> (ChessPiece) p)
                .filter(p -> p.getColor() == color && p instanceof King)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("There is no " + color + " king on the board"));
    }

    public boolean testCheck(Color color) {
        Position kingPosition = king(color).getChessPosition().toPosition();
        Color opponent = opponent(color);

        return piecesOnTheBoard.stream()
                .map(p -> (ChessPiece) p)
                .filter(p -> p.getColor() == opponent)
                .anyMatch(p -> {
                    boolean[][] moves = p.possibleMoves();
                    return moves[kingPosition.getRow()][kingPosition.getColumn()];
                });
    }

    public boolean testCheckmate(Color color, ChessPiece enPassantVulnerable) {
        if (!testCheck(color)) return false;

        List<ChessPiece> pieces = piecesOnTheBoard.stream()
                .map(p -> (ChessPiece) p)
                .filter(p -> p.getColor() == color)
                .collect(Collectors.toList());

        for (ChessPiece p : pieces) {
            boolean[][] mat = p.possibleMoves();
            for (int row = 0; row < board.getRows(); row++) {
                for (int col = 0; col < board.getColumns(); col++) {
                    if (!mat[row][col]) continue;

                    Position source   = p.getChessPosition().toPosition();
                    Position target   = new Position(row, col);
                    ChessPiece captured = executor.makeMove(source, target);
                    boolean stillInCheck = testCheck(color);
                    executor.undoMove(source, target, captured, enPassantVulnerable);

                    if (!stillInCheck) return false;
                }
            }
        }
        return true;
    }

    private Color opponent(Color color) {
        return (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
    }
}