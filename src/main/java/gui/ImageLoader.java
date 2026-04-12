package gui;

import chess.Color;
import chess.ChessPiece;
import chess.pieces.*;
import javafx.scene.image.Image;

import java.io.InputStream;

public class ImageLoader {

    public Image getPieceImage(ChessPiece piece) {
        if (piece == null) {
            return null;
        }

        // Dentro de ImageLoader.java
        String color = piece.getColor().toString(); // Retorna "WHITE" ou "BLACK"
        String type = piece.getClass().getSimpleName(); // Retorna "King", "Pawn", etc.

        // Caminho exato: /assets/pieces/WHITE_King.png
        String path = "/assets/pieces/" + color + "_" + type + ".png";
        System.out.println("Tentando carregar imagem: " + path);

        InputStream stream = getClass().getResourceAsStream(path);

        if (stream == null) {
            throw new RuntimeException("Imagem não encontrada: " + path);
        }

        return new Image(stream);
    }

    private String getPieceType(ChessPiece piece) {
        if (piece instanceof King) return "king";
        if (piece instanceof Queen) return "queen";
        if (piece instanceof Rook) return "rook";
        if (piece instanceof Bishop) return "bishop";
        if (piece instanceof Knight) return "knight";
        if (piece instanceof Pawn) return "pawn";

        throw new IllegalArgumentException(
                "Tipo de peça não reconhecido: " + piece.getClass().getSimpleName()
        );
    }
}