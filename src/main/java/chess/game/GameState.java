package chess.game;

/**
 * Define os possíveis estados de uma sessão de jogo.
 * Utilizado para controlar a interatividade da UI e o fluxo do GameManager.
 */
public enum GameState {

    /** A sessão foi criada mas o cronômetro e a IA ainda não foram disparados. */
    CREATED,

    /** Aguardando uma ação inicial ou carregamento de recursos/engine. */
    WAITING,

    /** O jogo está ativo e aceitando lances de jogadores humanos. */
    RUNNING,

    /** O motor de IA (Stockfish) está processando o melhor lance. */
    BOT_THINKING,

    /** O rei de um dos jogadores está sob ataque imediato (Xeque). */
    CHECK,

    /** O jogo foi interrompido temporariamente. */
    PAUSED,

    /** Fim de jogo: Rei em xeque sem movimentos legais (Xeque-mate). */
    CHECKMATE,

    /** Fim de jogo: Sem movimentos legais, mas o rei não está em xeque (Afogamento). */
    STALEMATE,

    /** Fim de jogo por empate (insuficiência de material ou repetição). */
    DRAW,

    /** A partida terminou por desistência ou decisão técnica. */
    FINISHED,

    /** A partida terminou por esgotamento de tempo. */
    TIMEOUT,

    /** A sessão foi encerrada e os recursos (processos externos) liberados. */
    DISPOSED;

    /**
     * Verifica se o estado atual permite que um humano interaja com as peças.
     * Bloqueia se o bot estiver pensando ou se o jogo acabou/pausou.
     */
    public boolean canPlayerMove() {
        return this == RUNNING || this == CHECK;
    }

    /**
     * Verifica se o estado indica que a IA está no controle do fluxo no momento.
     */
    public boolean isBotProcessing() {
        return this == BOT_THINKING;
    }

    /**
     * Verifica se a partida chegou a um estado terminal (não há mais lances possíveis).
     */
    public boolean isTerminal() {
        return this == CHECKMATE ||
                this == STALEMATE ||
                this == DRAW ||
                this == FINISHED ||
                this == TIMEOUT ||
                this == DISPOSED;
    }

    /**
     * Indica se a interface deve destacar o Rei ou aplicar efeitos de urgência.
     */
    public boolean isAlertState() {
        return this == CHECK || this == CHECKMATE;
    }

    /**
     * Retorna uma mensagem amigável para exibição na UI (statusLabel).
     */
    public String getStatusMessage() {
        return switch (this) {
            case CREATED -> "Aguardando início...";
            case WAITING -> "Inicializando motor...";
            case RUNNING -> "Sua vez de jogar";
            case BOT_THINKING -> "IA pensando...";
            case CHECK -> "XEQUE!";
            case PAUSED -> "Jogo pausado";
            case CHECKMATE -> "XEQUE-MATE!";
            case STALEMATE -> "EMPATE (Afogamento)";
            case DRAW -> "EMPATE";
            case TIMEOUT -> "FIM DE TEMPO";
            default -> "Partida encerrada";
        };
    }
}