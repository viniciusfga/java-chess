package chess.history;

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

    // --- Getters ---
    public ChessPiece getPiece() { return piece; }
    public Position getSource() { return source; }
    public Position getTarget() { return target; }
    public ChessPiece getCapturedPiece() { return capturedPiece; }
    public ChessPiece getPromotedPiece() { return promotedPiece; }
    public String getSanAnnotation() { return sanAnnotation; }

    public void setSanAnnotation(String sanAnnotation) {
        this.sanAnnotation = sanAnnotation;
    }

    /**
     * Gera a string UCI (ex: e2e4 ou a7a8q).
     * Essencial para o StockfishEngine.
     */
    public String getUciNotation() {
        // Usa o helper para converter as coordenadas da matriz para xadrez (ex: 7,4 -> e1)
        String notation = toUci(source) + toUci(target);

        // No UCI, a promoção é indicada por uma letra minúscula no fim
        if (promotedPiece != null) {
            notation += getPromotionChar(promotedPiece);
        }
        return notation;
    }

    /**
     * Converte uma Position (row, col) para string UCI (ex: a1, h8).
     */
    private String toUci(Position position) {
        char column = (char) ('a' + position.getColumn());
        int row = 8 - position.getRow();
        return "" + column + row;
    }

    /**
     * Mapeia a peça promovida para o caractere correspondente.
     */
    private String getPromotionChar(ChessPiece piece) {
        String name = piece.getClass().getSimpleName().toLowerCase();
        return switch (name) {
            case "queen"  -> "q";
            case "rook"   -> "r";
            case "bishop" -> "b";
            case "knight" -> "n";
            default       -> "q"; // Fallback para Rainha
        };
    }
}