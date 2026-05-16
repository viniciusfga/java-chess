package gui.view;

import chess.core.ChessPiece;
import gui.util.ImageLoader;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class ChessBoardView {

    private static final int SQUARE_SIZE = 80;
    private static final double PIECE_SIZE = SQUARE_SIZE * 0.90;

    private final ImageLoader imageLoader = new ImageLoader();

    public static int getSquareSize() {
        return SQUARE_SIZE;
    }

    // Raio do círculo de movimento vazio
    private static final double DOT_RADIUS = SQUARE_SIZE * 0.18;
    // Raio do anel de captura (cobre quase a casa inteira)
    private static final double CAPTURE_RADIUS = SQUARE_SIZE * 0.46;
    private static final double CAPTURE_STROKE = SQUARE_SIZE * 0.07;

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

        boolean isLight = (row + col) % 2 == 0;

        // ── 1. Fundo da casa ──────────────────────────────────────────────────
        Rectangle background = new Rectangle(SQUARE_SIZE, SQUARE_SIZE);

        if (isSource) {
            // Casa selecionada: amarelo
            background.setFill(Color.web(isLight ? "#f4d35e" : "#d4ac16"));
        } else {
            // Cor normal — mesma para casas com movimento possível
            background.setFill(isLight ? Color.web("#ebecd0") : Color.web("#779556"));
        }
        square.getChildren().add(background);

        // ── 2. Indicador de movimento possível ────────────────────────────────
        if (isPossibleMove) {
            if (isCaptureMove) {
                // Peça capturável: anel colorido ao redor da peça
                Circle ring = new Circle(CAPTURE_RADIUS);
                ring.setFill(Color.TRANSPARENT);
                ring.setStroke(Color.web("#000000", 0.20));
                ring.setStrokeWidth(CAPTURE_STROKE);
                square.getChildren().add(ring);
            } else {
                // Casa vazia: ponto central semitransparente
                Circle dot = new Circle(DOT_RADIUS);
                dot.setFill(Color.web("#000000", 0.20));
                square.getChildren().add(dot);
            }
        }

        // ── 3. Peça ───────────────────────────────────────────────────────────
        if (piece != null) {
            square.getChildren().add(createPieceImageView(piece));
        }

        return square;
    }

    public ImageView createPieceImageView(ChessPiece piece) {
        Image image = imageLoader.getPieceImage(piece);

        ImageView iv = new ImageView(image);
        iv.setFitWidth(PIECE_SIZE);
        iv.setFitHeight(PIECE_SIZE);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setPickOnBounds(false);
        return iv;
    }
}