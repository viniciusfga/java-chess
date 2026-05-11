# ♟️ Chess System

<p align="center">
  <img src="https://img.shields.io/badge/Java-21%2B-red?style=for-the-badge&logo=openjdk">
  <img src="https://img.shields.io/badge/Maven-Build-blue?style=for-the-badge&logo=apachemaven">
  <img src="https://img.shields.io/badge/JavaFX-GUI-0A84FF?style=for-the-badge">
  <img src="https://img.shields.io/badge/Status-Em%20Desenvolvimento-yellow?style=for-the-badge">
</p>

<p align="center">
  <strong>Sistema completo de xadrez desenvolvido em Java com arquitetura modular, JavaFX e integração futura com IA.</strong>
</p>

<p align="center">
  ♟️ Jogue • 🤖 Enfrente Bots • 🔁 Replay • 🧠 Estude Partidas
</p>

---

# 📌 Sobre o Projeto

O **Chess System** é um sistema completo de xadrez desenvolvido em **Java + Maven**, com foco em:

- Arquitetura limpa e modular
- Regras oficiais do xadrez
- Interface gráfica com JavaFX
- Histórico e replay de partidas
- Integração com engines de IA
- Organização profissional em pacotes
- Escalabilidade futura

O projeto foi criado com objetivo de aprofundar conhecimentos em:

- Programação Orientada a Objetos (POO)
- Arquitetura de Software
- Design de Sistemas
- JavaFX
- Separação de responsabilidades
- Integração com engines UCI
- Modelagem de domínio
- UML e documentação arquitetural

---

# ✨ Funcionalidades

## ♟️ Regras Oficiais

- Movimentação completa das peças
- Check
- Checkmate
- Stalemate
- Roque pequeno e grande
- En Passant
- Promoção de peões

---

## 🖥️ Interface

- Interface via terminal
- Interface gráfica com JavaFX
- Renderização do tabuleiro
- Sistema de carregamento de assets
- Controllers desacoplados

---

## 📜 Histórico e Replay

- Histórico completo de jogadas
- Undo / Redo
- Replay de partidas
- Geração SAN
- Exportação PGN

---

## 🤖 Inteligência Artificial

- Integração com Stockfish
- Comunicação via protocolo UCI
- Sistema preparado para múltiplas engines
- Configuração de dificuldade

---

# 🏗️ Arquitetura

O projeto utiliza uma arquitetura modular organizada em camadas.

```text
GUI Layer
    ↓
Game/Application Layer
    ↓
Core Chess Engine
    ↓
Board Infrastructure
```

---

# 📂 Estrutura do Projeto

```text
src/
└── main/
    ├── java/
    │   ├── application/
    │   ├── board/
    │   ├── chess/
    │   │   ├── ai/
    │   │   ├── core/
    │   │   ├── engine/
    │   │   ├── game/
    │   │   ├── history/
    │   │   ├── io/
    │   │   ├── pieces/
    │   │   └── utils/
    │   └── gui/
    │       └── controller/
    │
    └── resources/
        ├── assets/
        │   └── pieces/
        └── gui/
```

---

# 🧠 Principais Componentes

| Módulo | Responsabilidade |
|---|---|
| `application` | Inicialização da aplicação |
| `board` | Estrutura genérica de tabuleiro |
| `chess.core` | Domínio central do xadrez |
| `chess.engine` | Validação e execução de regras |
| `chess.game` | Gerenciamento da sessão |
| `chess.ai` | Integração com engines de IA |
| `chess.history` | Histórico e replay |
| `chess.io` | Exportação e persistência |
| `gui` | Interface JavaFX |

---

# 🧩 Tecnologias Utilizadas

<p align="left">
  <img src="https://skillicons.dev/icons?i=java,maven,git,github" />
</p>

- Java 17+
- Maven
- JavaFX
- PlantUML
- Stockfish (UCI Engine)

---

# 📸 Diagramas

O projeto possui documentação arquitetural utilizando PlantUML:

- Package Diagram
- Class Diagram
- Sequence Diagram
- State Diagram

```text
docs/
└── architecture/
```

---

# 🚀 Como Executar

## Pré-requisitos

- Java 21+
- Maven 3.9+
- JavaFX SDK

---

## Clone o projeto

```bash
git clone <https://github.com/viniciusfga/java-chess>
```

---

## Execute

```bash
mvn clean javafx:run
```

---

# 🎯 Objetivos do Projeto

- Criar uma engine de xadrez modular
- Aplicar boas práticas de arquitetura
- Estudar modelagem de domínio
- Trabalhar com interfaces gráficas
- Integrar IA via protocolo UCI
- Produzir documentação arquitetural profissional

---

# 📌 Roadmap

- [x] Estrutura base do projeto
- [x] Engine principal
- [x] Regras oficiais
- [x] Histórico de jogadas
- [x] Replay
- [ ] Multiplayer
- [ ] Ranked System
- [ ] Timer competitivo
- [ ] IA avançada
- [ ] Aberturas pré-programadas
- [ ] Análise automática de partidas

---

# 📚 Aprendizados

Este projeto explora conceitos importantes de engenharia de software:

- SOLID
- Clean Architecture
- MVC
- Domain Modeling
- UML
- JavaFX
- Integração com processos externos
- Design modular

---

# 👨‍💻 Autor

Desenvolvido por **Vinícius F.G. Araújo**  
🎓 Bacharelado em Sistemas de Informação

---

# ⭐ Status do Projeto

🚧 Projeto em desenvolvimento contínuo.