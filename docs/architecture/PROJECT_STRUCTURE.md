# Estrutura do Projeto

```text
docs/                       # Documentação, diagramas e materiais auxiliares
└── architecture/
     ├── packages/
     └── source.java 
    
src/
└── main/
    ├── java/
    │   ├── application/
    │   │   └── Launcher.java               # Main que chama o JavaFX
    │   │  
    │   ├── board/                          # Estrutura agnóstica de tabuleiro (Matrix)
    │   │   ├── Board.java         
    │   │   ├── BoardException.java     
    │   │   ├── Piece.java    
    │   │   └── Position.java       
    │   │   
    │   ├── chess/
    │   │   ├── ai/                         # Comunicação com o Bot
    │   │   │   ├── BotEngine.java          # Interface para futuras IAs
    │   │   │   ├── BotDifficulty.java      # Mapeamento de Elo/Params UCI
    │   │   │   ├── StockfishEngine.java    # Gerencia o Processo Stockfish
    │   │   │   ├── UCIMpapper.java    
    │   │   │   └── UCIProtocol.java        # Parser de strings para o padrão UCI
    │   │   ├── core/                       # Cérebro do Jogo
    │   │   │   ├── ChessMatch.java
    │   │   │   ├── ChessPiece.java
    │   │   │   ├── ChessPosition.java
    │   │   │   └── Color.java
    │   │   ├── engine/                     # Regras de execução e validação
    │   │   ├── game/                       # Regras de Sessão
    │   │   │   ├── GameConfig.java              
    │   │   │   ├── GameManager.java 
    │   │   │   ├── GameMode.java   
    │   │   │   ├── GameSession.java           
    │   │   │   └── GameState.java
    │   │   │       
    │   │   ├── history/                    # ChessLog e classes de Move
    │   │   ├── pieces/                     # Implementação de cada peça
    │   │   ├── io/                         # PGN, FileManagers
    │   │   ├── service/  
    │   │   │   ├── BotMoveService    
    │   │   │   ├── EngineGameService              
    │   │   │   └── MoveHistorySerializer.java 
    │   │   │   
    │   │   └── utils/                      # SANGenerator
    │   │
    │   └── gui/                            # Camada Visual (JavaFX)
    │       ├── controller/
    │       │    ├── GameController.java        # Controlador da partida
    │       │    └── GameSetupController.java   # Controlador da tela de opções
    │       │  
    │       ├── service/    
    │       │    ├── GameClockController.java   
    │       │    ├── MoveHistoryFormatter.java   
    │       │    ├── MoveSoundsService.java   
    │       │    ├── NavigationService.java  
    │       │    └── PieceAnimator.java  
    │       │       
    │       ├── ChessBoardView.java             # Renderização do grid
    │       └── ImageLoader.java                # Cache de asset
    └──resources/
        ├──assets.pieces
        └── gui/ 
            ├──GameView.fxml
            └──style