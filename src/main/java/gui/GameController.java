package gui;

import board.Position;
import chess.ChessException;
import chess.ChessMatch;
import chess.ChessPiece;
import chess.ChessPosition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

public class GameController {

    @FXML
    private GridPane boardGrid;

    @FXML
    private Label turnLabel;

    @FXML
    private Label statusLabel;

    private ChessMatch chessMatch;
    private ChessBoardView chessBoardView;

    private Position sourcePosition; // Usando Position para facilitar lógica de matriz
    private boolean[][] possibleMoves;

    @FXML
    public void initialize() {
        chessMatch = new ChessMatch();
        chessBoardView = new ChessBoardView();

        sourcePosition = null;
        possibleMoves = null;

        drawBoard();
        updateUI();
    }

    private void drawBoard() {
        boardGrid.getChildren().clear();
        ChessPiece[][] pieces = chessMatch.getPieces();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = pieces[row][col];

                // 1. Verificar se é a casa selecionada (Amarelo)
                boolean isSource = sourcePosition != null
                        && sourcePosition.getRow() == row
                        && sourcePosition.getColumn() == col;

                // 2. Verificar se é um movimento possível (Verde)
                boolean isPossibleMove = possibleMoves != null && possibleMoves[row][col];

                // 3. Verificar se é uma captura (Vermelho)
                boolean isCaptureMove = false;
                if (isPossibleMove && piece != null) {
                    // Se for possível mover para cá e houver uma peça, é captura
                    isCaptureMove = true;
                }

                StackPane square = chessBoardView.createSquare(
                        row,
                        col,
                        piece,
                        isSource,
                        isPossibleMove,
                        isCaptureMove
                );

                final int currentRow = row;
                final int currentCol = col;
                square.setOnMouseClicked(event -> handleSquareClick(currentRow, currentCol));

                boardGrid.add(square, col, row);
            }
        }
    }

    private void handleSquareClick(int row, int col) {
        Position clickedPosition = new Position(row, col);

        try {
            // SELEÇÃO DE ORIGEM
            if (sourcePosition == null) {
                ChessPiece piece = chessMatch.getPieces()[row][col];
                if (piece == null) {
                    statusLabel.setText("Nenhuma peça nesta casa.");
                    return;
                }

                // Valida se a peça é da cor do jogador atual (opcional, performChessMove já valida)
                selectPiece(clickedPosition, piece);
            }
            // CANCELAR SELEÇÃO (Clicar na mesma peça)
            else if (sourcePosition.equals(clickedPosition)) {
                clearSelection();
                statusLabel.setText("Seleção cancelada.");
            }
            // TROCAR SELEÇÃO (Clicar em outra peça da mesma cor)
            else if (isAllyPiece(row, col)) {
                ChessPiece piece = chessMatch.getPieces()[row][col];
                selectPiece(clickedPosition, piece);
            }
            // EXECUTAR MOVIMENTO
            else {
                ChessPosition source = ChessPosition.fromPosition(sourcePosition);
                ChessPosition target = ChessPosition.fromPosition(clickedPosition);

                chessMatch.performChessMove(source, target);

                clearSelection();
                updateUI();
            }
            drawBoard();

        } catch (ChessException e) {
            statusLabel.setText("Erro: " + e.getMessage());
            clearSelection();
            drawBoard();
        }
    }

    private void selectPiece(Position pos, ChessPiece piece) {
        sourcePosition = pos;
        possibleMoves = chessMatch.possibleMoves(ChessPosition.fromPosition(sourcePosition));
        statusLabel.setText("Peça selecionada: " + getFriendlyPieceName(piece) + " em " + ChessPosition.fromPosition(pos));
    }

    private void clearSelection() {
        sourcePosition = null;
        possibleMoves = null;
    }

    private boolean isAllyPiece(int row, int col) {
        ChessPiece clickedPiece = chessMatch.getPieces()[row][col];
        ChessPiece sourcePiece = chessMatch.getPieces()[sourcePosition.getRow()][sourcePosition.getColumn()];
        return clickedPiece != null && sourcePiece != null && clickedPiece.getColor() == sourcePiece.getColor();
    }

    private String getFriendlyPieceName(ChessPiece piece) {
        String simpleName = piece.getClass().getSimpleName();
        return switch (simpleName) {
            case "King" -> "Rei";
            case "Queen" -> "Rainha";
            case "Rook" -> "Torre";
            case "Bishop" -> "Bispo";
            case "Knight" -> "Cavalo";
            case "Pawn" -> "Peão";
            default -> simpleName;
        };
    }

    private void updateUI() {
        turnLabel.setText("Turno: " + chessMatch.getCurrentPlayer());
        if (chessMatch.getCheckMate()) statusLabel.setText("XEQUE-MATE!");
        else if (chessMatch.getCheck()) statusLabel.setText("XEQUE!");
        else statusLabel.setText("Partida em andamento");
    }

    @FXML
    public void onUndoAction() {
        // Lógica de desfazer aqui
        drawBoard();
        updateUI();
    }

    @FXML
    public void onRedoAction() {
        // Lógica de refazer aqui
        drawBoard();
        updateUI();
    }
}