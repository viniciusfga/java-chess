package gui.service;

import gui.view.ChessBoardView;
import chess.core.ChessPiece;
import board.Position;
import javafx.animation.TranslateTransition;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class PieceAnimator {

    private static final int ANIMATION_DURATION_MS = 70;

    public void animateMove(GridPane boardGrid, ChessBoardView boardView, ChessPiece piece,
                            Position source, Position target, Runnable onFinished) {
        if (piece == null || source == null || target == null) {
            onFinished.run();
            return;
        }

        ImageView animatedPiece = boardView.createPieceImageView(piece);
        animatedPiece.setMouseTransparent(true);

        int squareSize = ChessBoardView.getSquareSize();

        StackPane animationLayer = new StackPane(animatedPiece);
        animationLayer.setMouseTransparent(true);
        animationLayer.setMinSize(squareSize, squareSize);
        animationLayer.setPrefSize(squareSize, squareSize);
        animationLayer.setMaxSize(squareSize, squareSize);

        boardGrid.add(animationLayer, source.getColumn() + 1, source.getRow());

        TranslateTransition transition = new TranslateTransition(Duration.millis(ANIMATION_DURATION_MS), animatedPiece);
        transition.setFromX(0);
        transition.setFromY(0);
        transition.setToX((target.getColumn() - source.getColumn()) * squareSize);
        transition.setToY((target.getRow() - source.getRow()) * squareSize);

        transition.setOnFinished(event -> {
            boardGrid.getChildren().remove(animationLayer);
            onFinished.run();
        });

        transition.play();
    }
}