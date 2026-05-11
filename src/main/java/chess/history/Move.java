package chess.history;

import board.Piece;
import board.Position;
import chess.core.ChessPiece;

public class Move {

    private ChessPiece piece;
    private Position source;
    private Position target;
    private ChessPiece capturedPiece;
    private ChessPiece promotedPiece;
    private String sanAnnotation;

    private boolean castlingKingSide;
    private boolean castlingQueenSide;
    private boolean enPassant;
    private boolean check;
    private boolean checkmate;

    public Move(
            ChessPiece piece,
            Position source,
            Position target,
            ChessPiece capturedPiece,
            ChessPiece promotedPiece,
            String sanAnnotation,
            boolean castlingKingSide,
            boolean castlingQueenSide,
            boolean enPassant,
            boolean check,
            boolean checkmate
    ) {
        this.piece = piece;
        this.source = source;
        this.target = target;
        this.capturedPiece = capturedPiece;
        this.promotedPiece = promotedPiece;
        this.sanAnnotation = sanAnnotation;
        this.castlingKingSide = castlingKingSide;
        this.castlingQueenSide = castlingQueenSide;
        this.enPassant = enPassant;
        this.check = check;
        this.checkmate = checkmate;
    }

    public Piece getPiece() {
        return piece;
    }

    public Position getSource() {
        return source;
    }

    public Position getTarget() {
        return target;
    }

    public ChessPiece getCapturedPiece() {
        return capturedPiece;
    }

    public ChessPiece getPromotedPiece() {
        return promotedPiece;
    }

    public String getSanAnnotation() {
        return sanAnnotation;
    }

    public boolean isCastlingKingSide() {
        return castlingKingSide;
    }

    public boolean isCastlingQueenSide() {
        return castlingQueenSide;
    }

    public boolean isEnPassant() {
        return enPassant;
    }

    public boolean isCheck() {
        return check;
    }

    public boolean isCheckmate() {
        return checkmate;
    }

    public void setSanAnnotation(String sanAnnotation) {
        this.sanAnnotation = sanAnnotation;
    }

    public String getUciNotation() {
        return toUci(getSource()) + toUci(getTarget());
    }

    private String toUci(Position position) {

        char column = (char) ('a' + position.getColumn());
        int row = 8 - position.getRow();

        return "" + column + row;
    }
}