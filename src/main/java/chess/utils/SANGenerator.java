package chess.utils;

import board.Position;
import chess.ChessMatch;
import chess.ChessPiece;
import chess.history.Move;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Knight;
import chess.pieces.Pawn;
import chess.pieces.Queen;
import chess.pieces.Rook;

import java.util.ArrayList;
import java.util.List;

public class SANGenerator {

    public static String generateSan(ChessMatch match, Move move) {
        ChessPiece movedPiece = (ChessPiece) move.getPiece();
        Position source = move.getSource();
        Position target = move.getTarget();
        ChessPiece capturedPiece = (ChessPiece) move.getCapturedPiece();
        ChessPiece promotedPiece = (ChessPiece) move.getPromotedPiece();

        boolean castlingKingSide = movedPiece instanceof chess.pieces.King
                && target.getColumn() == source.getColumn() + 2;

        boolean castlingQueenSide = movedPiece instanceof chess.pieces.King
                && target.getColumn() == source.getColumn() - 2;

        boolean enPassant = movedPiece instanceof Pawn
                && source.getColumn() != target.getColumn()
                && capturedPiece != null;

        return generateSan(
                match,
                movedPiece,
                source,
                target,
                capturedPiece,
                promotedPiece,
                castlingKingSide,
                castlingQueenSide,
                enPassant,
                match.getCheck(),
                match.getCheckMate()
        );
    }
    public static String generateSan(
            ChessMatch match,
            ChessPiece movedPiece,
            Position source,
            Position target,
            ChessPiece capturedPiece,
            ChessPiece promotedPiece,
            boolean castlingKingSide,
            boolean castlingQueenSide,
            boolean enPassant,
            boolean check,
            boolean checkmate
    ) {
        if (castlingKingSide) {
            return appendCheckSuffix("O-O", check, checkmate);
        }

        if (castlingQueenSide) {
            return appendCheckSuffix("O-O-O", check, checkmate);
        }

        StringBuilder san = new StringBuilder();
        boolean isPawn = movedPiece instanceof Pawn;

        if (!isPawn) {
            san.append(pieceLetter(movedPiece));
            san.append(disambiguation(match, movedPiece, source, target));
        } else if (capturedPiece != null || enPassant) {
            san.append((char) ('a' + source.getColumn()));
        }

        if (capturedPiece != null || enPassant) {
            san.append("x");
        }

        san.append(toSquare(target));

        if (promotedPiece != null) {
            san.append("=").append(pieceLetter(promotedPiece));
        }

        return appendCheckSuffix(san.toString(), check, checkmate);
    }

    private static String appendCheckSuffix(String san, boolean check, boolean checkmate) {
        if (checkmate) {
            return san + "#";
        }
        if (check) {
            return san + "+";
        }
        return san;
    }

    private static String pieceLetter(ChessPiece piece) {
        if (piece instanceof Knight) return "N";
        if (piece instanceof Bishop) return "B";
        if (piece instanceof Rook) return "R";
        if (piece instanceof Queen) return "Q";
        if (piece instanceof King) return "K";
        return "";
    }

    private static String toSquare(Position position) {
        char column = (char) ('a' + position.getColumn());
        int row = 8 - position.getRow();
        return "" + column + row;
    }

    private static String disambiguation(ChessMatch match, ChessPiece movedPiece, Position source, Position target) {
        List<ChessPiece> candidates = new ArrayList<>();
        ChessPiece[][] board = match.getPieces();

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                ChessPiece p = board[i][j];

                if (p == null || p == movedPiece) {
                    continue;
                }

                if (p.getColor() == movedPiece.getColor()
                        && p.getClass().equals(movedPiece.getClass())) {

                    boolean[][] moves = p.possibleMoves();
                    if (moves[target.getRow()][target.getColumn()]) {
                        candidates.add(p);
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return "";
        }

        boolean sameColumn = false;
        boolean sameRow = false;

        for (ChessPiece p : candidates) {
            Position otherSource = p.getChessPosition().toPosition();

            if (otherSource.getColumn() == source.getColumn()) {
                sameColumn = true;
            }

            if (otherSource.getRow() == source.getRow()) {
                sameRow = true;
            }
        }

        char file = (char) ('a' + source.getColumn());
        int rank = 8 - source.getRow();

        if (!sameColumn) {
            return String.valueOf(file);
        }

        if (!sameRow) {
            return String.valueOf(rank);
        }

        return "" + file + rank;
    }
}