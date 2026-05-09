package chess.engine;

import board.Board;
import board.Piece;
import board.Position;
import chess.core.ChessPiece;
import chess.core.Color;
import chess.pieces.King;
import chess.pieces.Pawn;

import java.util.List;

public class MoveExecutor {

    private final Board board;
    private final List<Piece> piecesOnTheBoard;
    private final List<Piece> capturedPieces;

    public MoveExecutor(Board board, List<Piece> piecesOnTheBoard, List<Piece> capturedPieces) {
        this.board = board;
        this.piecesOnTheBoard = piecesOnTheBoard;
        this.capturedPieces = capturedPieces;
    }

    public ChessPiece makeMove(Position source, Position target) {
        ChessPiece p = (ChessPiece) board.removePiece(source);
        p.increaseMoveCount();

        ChessPiece capturedPiece = (ChessPiece) board.removePiece(target);
        board.placePiece(p, target);

        if (capturedPiece != null) {
            piecesOnTheBoard.remove(capturedPiece);
            capturedPieces.add(capturedPiece);
        }

        if (p instanceof King && target.getColumn() == source.getColumn() + 2) {
            moveCastlingRook(source.getRow(), source.getColumn() + 3, source.getColumn() + 1, true);
        }

        if (p instanceof King && target.getColumn() == source.getColumn() - 2) {
            moveCastlingRook(source.getRow(), source.getColumn() - 4, source.getColumn() - 1, true);
        }

        if (p instanceof Pawn && source.getColumn() != target.getColumn() && capturedPiece == null) {
            capturedPiece = captureEnPassantPawn(p.getColor(), target);
        }

        return capturedPiece;
    }

    public void undoMove(Position source, Position target, ChessPiece capturedPiece, ChessPiece enPassantVulnerable) {
        ChessPiece p = (ChessPiece) board.removePiece(target);
        p.decreaseMoveCount();
        board.placePiece(p, source);

        if (capturedPiece != null) {
            boolean wasEnPassant = p instanceof Pawn
                    && source.getColumn() != target.getColumn()
                    && capturedPiece == enPassantVulnerable;

            if (wasEnPassant) {
                int enPassantRow = (p.getColor() == Color.WHITE) ? 3 : 4;
                board.placePiece(capturedPiece, new Position(enPassantRow, target.getColumn()));
            } else {
                board.placePiece(capturedPiece, target);
            }

            capturedPieces.remove(capturedPiece);
            piecesOnTheBoard.add(capturedPiece);
        }

        if (p instanceof King && target.getColumn() == source.getColumn() + 2) {
            moveCastlingRook(source.getRow(), source.getColumn() + 1, source.getColumn() + 3, false);
        }

        if (p instanceof King && target.getColumn() == source.getColumn() - 2) {
            moveCastlingRook(source.getRow(), source.getColumn() - 1, source.getColumn() - 4, false);
        }
    }

    private void moveCastlingRook(int row, int fromColumn, int toColumn, boolean increasing) {
        Position from = new Position(row, fromColumn);
        Position to   = new Position(row, toColumn);
        ChessPiece rook = (ChessPiece) board.removePiece(from);
        board.placePiece(rook, to);
        if (increasing) rook.increaseMoveCount();
        else            rook.decreaseMoveCount();
    }

    private ChessPiece captureEnPassantPawn(Color color, Position target) {
        int captureRow = (color == Color.WHITE) ? target.getRow() + 1 : target.getRow() - 1;
        Position pawnPosition = new Position(captureRow, target.getColumn());
        ChessPiece captured = (ChessPiece) board.removePiece(pawnPosition);
        if (captured != null) {
            capturedPieces.add(captured);
            piecesOnTheBoard.remove(captured);
        }
        return captured;
    }
}