package chess.ai;

import chess.core.Color;

import java.io.IOException;
import java.util.Optional;

/**
 * Interface que abstrai qualquer motor de IA de xadrez.
 * Permite trocar o Stockfish por outro engine sem alterar o restante da aplicação.
 */
public interface BotEngine {

    /**
     * Inicializa o engine (abre processo, handshake de protocolo, etc).
     *
     * @throws IOException se o engine não puder ser iniciado
     */
    void start() throws IOException;

    /**
     * Encerra o engine de forma limpa.
     */
    void stop();

    /**
     * Retorna true enquanto o engine estiver pronto para receber comandos.
     */
    boolean isReady();

    /**
     * Informa a posição atual ao engine via FEN.
     *
     * @param fen  posição completa em notação FEN
     * @param moves lista de movimentos em notação UCI feitos a partir do FEN
     *               (ex.: "e2e4 e7e5"), pode ser vazia
     */
    void setPosition(String fen, String moves);

    /**
     * Pede ao engine o melhor movimento para a posição atual.
     *
     * @param difficulty nível de dificuldade / parâmetros de busca
     * @param color      cor que o bot vai jogar (usado para validação futura)
     * @return movimento em notação UCI (ex.: "e2e4"), ou empty se o engine falhar
     */
    Optional<String> getBestMove(BotDifficulty difficulty, Color color);

    /**
     * Interrompe a busca atual, se houver uma em andamento.
     */
    void stopSearch();

    /**
     * Nome descritivo do engine (para logs e UI).
     */
    String getName();
}