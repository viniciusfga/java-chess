package gui;

import chess.ChessPiece;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.net.URL;

public class ChessBoardView {

    private static final int SQUARE_SIZE = 80;
    private static final double PIECE_SIZE = SQUARE_SIZE * 0.90;

    public StackPane createSquare(int row, int col,
                                  ChessPiece piece,
                                  boolean isSource,
                                  boolean isPossibleMove,
                                  boolean isCaptureMove) {

        StackPane square = new StackPane();
        square.setMinSize(SQUARE_SIZE, SQUARE_SIZE);
        square.setPrefSize(SQUARE_SIZE, SQUARE_SIZE);
        square.setMaxSize(SQUARE_SIZE, SQUARE_SIZE);
        square.setAlignment(Pos.CENTER);

        Rectangle background = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);

        boolean isLightSquare = (row + col) % 2 == 0;

        if (isSource) {
            background.setFill(Color.web("#f4d35e"));
        } else if (isCaptureMove) {
            background.setFill(Color.web("#d9534f"));
        } else if (isPossibleMove) {
            background.setFill(Color.web("#7ecb77"));
        } else {
            background.setFill(isLightSquare
                    ? Color.web("#ebecd0")
                    : Color.web("#779556"));
        }

        square.getChildren().add(background);

        if (piece != null) {
            ImageView pieceImageView = createPieceImageView(piece);
            square.getChildren().add(pieceImageView);
        }

        return square;
    }

    private ImageView createPieceImageView(ChessPiece piece) {
        String color = piece.getColor().toString();
        String type = piece.getClass().getSimpleName();
        String path = "/assets/pieces/" + color + "_" + type + ".png";

        URL imageUrl = getClass().getResource(path);
        if (imageUrl == null) {
            System.err.println("Imagem não encontrada: " + path);
            return new ImageView();
        }

        Image image = new Image(imageUrl.toExternalForm());
        ImageView imageView = new ImageView(image);

        imageView.setFitWidth(PIECE_SIZE);
        imageView.setFitHeight(PIECE_SIZE);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setPickOnBounds(false);

        return imageView;
    }
}