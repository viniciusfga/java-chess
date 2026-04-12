classDiagram
%% =========================
%% CAMADA DE TABULEIRO
%% =========================

    class Position {
        -int row
        -int column
        +Position(int row, int column)
        +setValues(int row, int column) void
        +toString() String
    }

    class Piece {
        <<abstract>>
        #Position position
        #Board board
        +Piece(Board board)
        +possibleMoves() boolean[][]
        +possibleMove(Position position) boolean
        +isThereAnyPossibleMove() boolean
    }

    class Board {
        -int rows
        -int columns
        -Piece[][] pieces
        +Board(int rows, int columns)
        +getRows() int
        +getColumns() int
        +piece(int row, int column) Piece
        +piece(Position position) Piece
        +placePiece(Piece piece, Position position) void
        +removePiece(Position position) Piece
        +positionExists(Position position) boolean
        +thereIsAPiece(Position position) boolean
    }

    class BoardException {
        +BoardException(String msg)
    }

    %% =========================
    %% CAMADA DE XADREZ
    %% =========================

    class Color {
        <<enumeration>>
        WHITE
        BLACK
    }

    class ChessPiece {
        <<abstract>>
        -Color color
        -int moveCount
        +ChessPiece(Board board, Color color)
        +getColor() Color
        +getMoveCount() int
        +getChessPosition() ChessPosition
        +increaseMoveCount() void
        +decreaseMoveCount() void
        #isThereOpponentPiece(Position position) boolean
    }

    class ChessMatch {
        -int turn
        -Color currentPlayer
        -Board board
        -boolean check
        -boolean checkMate
        -ChessPiece enPassantVulnerable
        -ChessPiece promoted
        +ChessMatch()
        +getTurn() int
        +getCurrentPlayer() Color
        +getCheck() boolean
        +getCheckMate() boolean
        +getPieces() ChessPiece[][]
        +possibleMoves(ChessPosition sourcePosition) boolean[][]
        +performChessMove(ChessPosition source, ChessPosition target) ChessPiece
        +replacePromotedPiece(String type) ChessPiece
        +performChessMove(source, target)
    }

    class ChessPosition {
        -char column
        -int row
        +ChessPosition(char column, int row)
        +getColumn() char
        +getRow() int
        #toPosition() Position
        +fromPosition(Position position) ChessPosition
        +toString() String
    }

    class ChessException {
        +ChessException(String msg)
    }

    class King {
        -ChessMatch chessMatch
        +King(Board board, Color color, ChessMatch chessMatch)
        +possibleMoves() boolean[][]
        +toString() String
    }

    class Queen {
        +Queen(Board board, Color color)
        +possibleMoves() boolean[][]
        +toString() String
    }

    class Rook {
        +Rook(Board board, Color color)
        +possibleMoves() boolean[][]
        +toString() String
    }

    class Bishop {
        +Bishop(Board board, Color color)
        +possibleMoves() boolean[][]
        +toString() String
    }

    class Knight {
        +Knight(Board board, Color color)
        +possibleMoves() boolean[][]
        +toString() String
    }

    class Pawn {
        -ChessMatch chessMatch
        +Pawn(Board board, Color color, ChessMatch chessMatch)
        +possibleMoves() boolean[][]
        +toString() String
    }

    %% =========================
    %% REGISTRO DE XADREZ
    %% =========================

    class ChessLog {
        -List<Move> moves
        +addMove(Move move)
        +getMoves() List~Move~
        +getFullHistory() List~String~
        +toPgn(String, String, String, String, String) String
    }

    class Move {
        -ChessPiece piece
        -Position source
        -Position target
        -ChessPiece capturedPiece
        -ChessPiece promotedPiece
        -String sanAnnotation
        -boolean castlingKingSide
        -boolean castlingQueenSide
        -boolean enPassant
        -boolean check
        -boolean checkmate
        +getSanAnnotation() String
    }

    class SanGenerator {
        <<Utility>>
        +generateSan(match, movedPiece, source, target, ...) String
        -appendCheckSuffix(String, boolean, boolean) String
        -pieceLetter(ChessPiece) String
        -toSquare(Position) String
        -disambiguation(...) String
    }

    %% =========================
    %% HERANÇA
    %% =========================

    ChessMatch ..> SanGenerator : utiliza para criar string
    ChessMatch --> ChessLog : registra lances
    ChessLog "1" *-- "many" Move : armazena
    Move --> "1" Position : source/target
    Move --> "1" ChessPiece : piece/captured
    
    ChessPiece --|> Piece
    King --|> ChessPiece
    Queen --|> ChessPiece
    Rook --|> ChessPiece
    Bishop --|> ChessPiece
    Knight --|> ChessPiece
    Pawn --|> ChessPiece

    BoardException --|> RuntimeException
    ChessException --|> BoardException

    %% =========================
    %% ASSOCIAÇÕES
    %% =========================

    Piece --> Position : position
    Piece --> Board : board
    Board "1" *-- "0..*" Piece : contains
    ChessPiece --> Color
    ChessPiece --> ChessPosition
    ChessPosition --> Position : conversao
    ChessMatch --> Board
    ChessMatch --> ChessPiece
    King --> ChessMatch
    Pawn --> ChessMatch