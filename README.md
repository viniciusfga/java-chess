# ♟️ Chess System

Sistema completo de jogo de xadrez desenvolvido em Java utilizando Maven, com suporte a:

- Regras oficiais do xadrez
- Interface via terminal
- Interface gráfica JavaFX
- Histórico de jogadas
- Exportação PGN
- Undo/Redo
- Replay de partidas
- Promoção de peças
- Roque
- En Passant
- Check e Checkmate

---

# 📌 Visão Geral

O projeto foi estruturado utilizando princípios de orientação a objetos, separação de responsabilidades e modularização por domínio.

A aplicação possui duas formas de interação:

- Interface via terminal (`console`)
- Interface gráfica (`gui`)

Além disso, implementa toda a lógica de regras do xadrez em uma camada isolada (`chess`), permitindo evolução futura do sistema.

---

# 🧱 Estrutura do Projeto

```text
.idea/                      # Configurações da IDE IntelliJ

docs/                       # Documentação, diagramas e materiais auxiliares

src/
└── main/
    ├── java/
    │   ├── application/        # Classe principal e inicialização
    │   ├── board/              # Estruturas base do tabuleiro
    │   ├── chess/              # Regras e entidades do xadrez
    │   │   ├── core/
    │   │   │   ├── ChessMatch.java
    │   │   │   ├── ChessPiece.java
    │   │   │   └── Color.java
    │   │   ├── engine/
    │   │   │   ├── MoveExecutor.java
    │   │   │   ├── GameStateEvaluator.java
    │   │   │   └── BoardInitializer.java
    │   │   ├── history/        # já existente
    │   │   ├── pieces/         # já existente
    │   │   ├── io/
    │   │   │   ├── PGNExporter.java
    │   │   │   └── ReplayManager.java
    │   │   └── utils/
    │   │       └── SANGenerator.java
    │   ├── console/            # Interface via terminal
    │   └── gui/                # Interface gráfica JavaFX
    │
    └── resources/              # Assets, imagens e configurações