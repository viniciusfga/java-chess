package chess.core;

import board.Board;
import board.Piece;
import board.Position;
import chess.ChessException;
import chess.engine.BoardInitializer;
import chess.engine.GameStateEvaluator;
import chess.engine.MoveExecutor;
import chess.history.ChessLog;
import chess.history.Move;
import chess.history.MoveHistory;
import chess.pieces.*;
import chess.utils.SANGenerator;

import java.util.ArrayList;
import java.util.List;

public class ChessMatch {

    private int turn;
    private Color currentPlayer;
    private Board board;
    private boolean check;
    private boolean checkmate;
    private ChessPiece enPassantVulnerable;
    private ChessPiece promoted;
    private ChessPiece lastPromotedPiece;

    private final ChessLog chessLog   = new ChessLog();
    private final MoveHistory history = new MoveHistory();

    private final List<Piece> piecesOnTheBoard = new ArrayList<>();
    private final List<Piece> capturedPieces   = new ArrayList<>();

    private final MoveExecutor executor;
    private final GameStateEvaluator evaluator;

    public ChessMatch() {
        board         = new Board(8, 8);
        turn          = 1;
        currentPlayer = Color.WHITE;
        executor      = new MoveExecutor(board, piecesOnTheBoard, capturedPieces);
        evaluator     = new GameStateEvaluator(board, piecesOnTheBoard, executor);
        new BoardInitializer(board, piecesOnTheBoard, this).setup();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getTurn()                          { return turn; }
    public Color getCurrentPlayer()               { return currentPlayer; }
    public boolean getCheck()                     { return check; }
    public boolean getCheckMate()                 { return checkmate; }
    public ChessPiece getEnPassantVulnerable()    { return enPassantVulnerable; }
    public ChessPiece getPromoted()               { return promoted; }
    public ChessLog getChessLog()                 { return chessLog; }
    public ChessPiece getLastPromotedPiece()      { return lastPromotedPiece; }

    public ChessPiece[][] getPieces() {
        ChessPiece[][] mat = new ChessPiece[board.getRows()][board.getColumns()];
        for (int i = 0; i < board.getRows(); i++)
            for (int j = 0; j < board.getColumns(); j++)
                mat[i][j] = (ChessPiece) board.piece(i, j);
        return mat;
    }

    // ── Movimentação ─────────────────────────────────────────────────────────

    public boolean[][] possibleMoves(ChessPosition sourcePosition) {
        Position position = sourcePosition.toPosition();
        validateSourcePosition(position);
        return board.piece(position).possibleMoves();
    }

    public ChessPiece performChessMove(ChessPosition sourcePosition, ChessPosition targetPosition) {
        Position source = sourcePosition.toPosition();
        Position target = targetPosition.toPosition();

        validateSourcePosition(source);
        validateTargetPosition(source, target);

        ChessPiece capturedPiece = executor.makeMove(source, target);

        if (evaluator.testCheck(currentPlayer)) {
            executor.undoMove(source, target, capturedPiece, enPassantVulnerable);
            throw new ChessException("You can't put yourself in check");
        }

        ChessPiece movedPiece = (ChessPiece) board.piece(target);

        boolean castlingKingSide  = movedPiece instanceof King && target.getColumn() == source.getColumn() + 2;
        boolean castlingQueenSide = movedPiece instanceof King && target.getColumn() == source.getColumn() - 2;
        boolean enPassant         = movedPiece instanceof Pawn
                && source.getColumn() != target.getColumn()
                && capturedPiece != null;

        promoted         = null;
        lastPromotedPiece = null;

        if (movedPiece instanceof Pawn && isPawnPromotion(movedPiece, target)) {
            lastPromotedPiece = movedPiece;
            promoted          = (ChessPiece) board.piece(target);
        }

        check = evaluator.testCheck(opponent(currentPlayer));

        if (evaluator.testCheckmate(opponent(currentPlayer), enPassantVulnerable)) {
            checkmate = true;
        } else {
            nextTurn();
        }

        enPassantVulnerable = (movedPiece instanceof Pawn
                && Math.abs(target.getRow() - source.getRow()) == 2)
                ? movedPiece : null;

        ChessPiece pieceForMove = (lastPromotedPiece != null) ? lastPromotedPiece : movedPiece;

        String san = SANGenerator.generateSan(
                this, (ChessPiece) pieceForMove, source, target,
                capturedPiece, promoted,
                castlingKingSide, castlingQueenSide, enPassant, check, checkmate);

        Move move = new Move(
                pieceForMove, source, target, capturedPiece, promoted, san,
                castlingKingSide, castlingQueenSide, enPassant, check, checkmate);

        history.push(move);

        return capturedPiece;
    }

    public ChessPiece replacePromotedPiece(String type) {
        if (promoted == null) throw new IllegalStateException("There is no piece to be promoted");
        if (!type.equals("B") && !type.equals("N") && !type.equals("R") && !type.equals("Q")) return promoted;

        Position pos = promoted.getChessPosition().toPosition();
        Piece p = board.removePiece(pos);
        piecesOnTheBoard.remove(p);

        ChessPiece newPiece = newPiece(type, promoted.getColor());
        board.placePiece(newPiece, pos);
        piecesOnTheBoard.add(newPiece);

        return newPiece;
    }

    // ── Undo / Redo ──────────────────────────────────────────────────────────

    public void undoLastMove() {
        Move move = history.undo();
        if (move == null) return;

        // Se houve promoção, troca a peça promovida de volta pelo peão original
        if (move.getPromotedPiece() != null) {
            ChessPiece promotedOnBoard = (ChessPiece) board.removePiece(move.getTarget());
            piecesOnTheBoard.remove(promotedOnBoard);
            board.placePiece(move.getPiece(), move.getTarget());
            piecesOnTheBoard.add(move.getPiece());
        }

        executor.undoMove(move.getSource(), move.getTarget(), move.getCapturedPiece(), enPassantVulnerable);
        chessLog.removeLastMove();
        undoTurn();
    }

    public void redoMove() {
        Move move = history.redo();
        if (move == null) return;

        executor.makeMove(move.getSource(), move.getTarget());

        if (move.getPromotedPiece() != null) {
            board.removePiece(move.getTarget());
            board.placePiece(move.getPromotedPiece(), move.getTarget());
        }

        nextTurn();
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private boolean isPawnPromotion(ChessPiece pawn, Position target) {
        return (pawn.getColor() == Color.WHITE && target.getRow() == 0)
                || (pawn.getColor() == Color.BLACK && target.getRow() == 7);
    }

    private ChessPiece newPiece(String type, Color color) {
        return switch (type) {
            case "B" -> new Bishop(board, color);
            case "N" -> new Knight(board, color);
            case "Q" -> new Queen(board, color);
            default  -> new Rook(board, color);
        };
    }

    private void validateSourcePosition(Position position) {
        if (!board.thereIsAPiece(position))
            throw new ChessException("There is no piece on source position");
        if (currentPlayer != ((ChessPiece) board.piece(position)).getColor())
            throw new ChessException("The chosen piece is not yours");
        if (!board.piece(position).isThereAnyPossibleMove())
            throw new ChessException("There is no possible moves for the chosen piece");
    }

    private void validateTargetPosition(Position source, Position target) {
        if (!board.piece(source).possibleMove(target))
            throw new ChessException("The chosen piece can't move to target position");
    }

    private void nextTurn() {
        turn++;
        currentPlayer = opponent(currentPlayer);
    }

    private void undoTurn() {
        turn--;
        currentPlayer = opponent(currentPlayer);
    }

    private Color opponent(Color color) {
        return (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
    }
}