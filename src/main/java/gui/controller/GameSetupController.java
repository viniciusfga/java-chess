package gui.controller;

import chess.ai.BotDifficulty;
import chess.core.Color;
import chess.game.GameConfig;
import chess.game.GameMode;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;

/**
 * Controlador da tela de configuração de partida.
 *
 * <h2>Fluxo</h2>
 * <pre>
 *   SetupView.fxml → GameSetupController → GameConfig → GameController.setup(config)
 * </pre>
 *
 * <h2>FXML esperado</h2>
 * O arquivo {@code SetupView.fxml} deve declarar os {@code fx:id} listados nos campos
 * {@code @FXML} abaixo. Os ToggleGroups {@code modeGroup} e {@code colorGroup} devem
 * ser declarados como atributo {@code toggleGroup} dos RadioButtons correspondentes.
 */
public class GameSetupController {

    // ── Modo de jogo ─────────────────────────────────────────────────────────
    @FXML private ToggleGroup modeGroup;
    @FXML private RadioButton pvpRadio;
    @FXML private RadioButton pveRadio;
    @FXML private RadioButton eveRadio;

    // ── Cor do jogador (apenas PvE) ──────────────────────────────────────────
    @FXML private VBox colorBox;
    @FXML private ToggleGroup colorGroup;
    @FXML private RadioButton whiteRadio;
    @FXML private RadioButton blackRadio;

    // ── Dificuldade da IA (PvE e EvE) ───────────────────────────────────────
    @FXML private VBox       difficultyBox;
    @FXML private ComboBox<BotDifficulty> difficultyCombo;
    @FXML private Label      difficultyDescLabel;

    // ── Caminho do Stockfish ─────────────────────────────────────────────────
    @FXML private TextField  stockfishPathField;

    // ── Controle de tempo ────────────────────────────────────────────────────
    @FXML private CheckBox   timedGameCheck;
    @FXML private VBox       timeControlBox;
    @FXML private TextField  minutesField;
    @FXML private TextField  incrementField;

    // ── Opções gerais ────────────────────────────────────────────────────────
    @FXML private CheckBox   allowUndoCheck;
    @FXML private CheckBox   showLegalMovesCheck;
    @FXML private CheckBox   soundCheck;

    // ── Rodapé ───────────────────────────────────────────────────────────────
    @FXML private Label      errorLabel;
    @FXML private Button     startButton;

    // ────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupDifficultyCombo();
        applyDefaults();
        bindVisibility();
        updateVisibility();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Setup interno
    // ════════════════════════════════════════════════════════════════════════

    private void setupDifficultyCombo() {
        difficultyCombo.getItems().addAll(BotDifficulty.values());
        difficultyCombo.setValue(BotDifficulty.MEDIUM);

        // Exibe nome amigável no ComboBox
        difficultyCombo.setButtonCell(new DifficultyCell());
        difficultyCombo.setCellFactory(lv -> new DifficultyCell());

        // Atualiza a descrição quando o nível muda
        difficultyCombo.valueProperty().addListener((obs, old, val) -> updateDifficultyDesc(val));
        updateDifficultyDesc(BotDifficulty.MEDIUM);
    }

    private void applyDefaults() {
        pvpRadio.setSelected(true);
        whiteRadio.setSelected(true);
        allowUndoCheck.setSelected(true);
        showLegalMovesCheck.setSelected(true);
        soundCheck.setSelected(true);
        minutesField.setText("10");
        incrementField.setText("0");
        stockfishPathField.setText("engines/stockfish.exe");
    }

    private void bindVisibility() {
        modeGroup.selectedToggleProperty().addListener((obs, old, nv) -> updateVisibility());
        timedGameCheck.selectedProperty().addListener((obs, old, nv) -> updateVisibility());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Visibilidade dinâmica
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Mostra/oculta seções conforme o modo selecionado e as opções marcadas.
     * Usa {@code setManaged()} para que o layout recalcule o espaço.
     */
    private void updateVisibility() {
        GameMode mode = getSelectedMode();

        boolean isPvE   = mode == GameMode.PVE;
        boolean needsAI = mode == GameMode.PVE || mode == GameMode.EVE;
        boolean timed   = timedGameCheck.isSelected();

        setVisibleAndManaged(colorBox,       isPvE);
        setVisibleAndManaged(difficultyBox,  needsAI);
        setVisibleAndManaged(timeControlBox, timed);

        // Undo não faz sentido em EvE
        allowUndoCheck.setDisable(mode == GameMode.EVE);
        if (mode == GameMode.EVE) allowUndoCheck.setSelected(false);
    }

    private static void setVisibleAndManaged(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Handlers FXML
    // ════════════════════════════════════════════════════════════════════════

    /** Abre um seletor de arquivo para localizar o executável do Stockfish. */
    @FXML
    private void onBrowseStockfish() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Localizar executável do Stockfish");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Executável", "*.exe", "*"),
                new FileChooser.ExtensionFilter("Todos os arquivos", "*.*")
        );
        File file = fc.showOpenDialog(startButton.getScene().getWindow());
        if (file != null) {
            stockfishPathField.setText(file.getAbsolutePath());
        }
    }

    /** Valida, constrói o {@link GameConfig} e inicia a partida. */
    @FXML
    private void onStartGame() {
        clearError();
        try {
            GameConfig config = buildConfig();
            launchGame(config);
        } catch (ValidationException e) {
            showError(e.getMessage());
        } catch (IOException e) {
            showError("Erro ao carregar a tela de jogo: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Construção do GameConfig
    // ════════════════════════════════════════════════════════════════════════

    private GameConfig buildConfig() {
        GameMode mode        = getSelectedMode();
        Color    humanColor  = whiteRadio.isSelected() ? Color.WHITE : Color.BLACK;
        BotDifficulty diff   = difficultyCombo.getValue();

        GameConfig.Builder builder = new GameConfig.Builder()
                .mode(mode)
                .humanColor(humanColor)
                .difficulty(diff)
                .allowUndo(allowUndoCheck.isSelected())
                .showLegalMoves(showLegalMovesCheck.isSelected())
                .soundEnabled(soundCheck.isSelected());

        // Caminho opcional do Stockfish
        String sfPath = stockfishPathField.getText().strip();
        if (!sfPath.isBlank()) {
            builder.stockfishPath(sfPath);
        }

        // Controle de tempo
        if (timedGameCheck.isSelected()) {
            int minutes   = parsePositiveInt(minutesField.getText(),   "Tempo por jogador (min)");
            int increment = parseNonNegativeInt(incrementField.getText(), "Incremento (s)");
            builder.totalTimeSeconds(minutes * 60).incrementSeconds(increment);
        }

        try {
            return builder.build();
        } catch (IllegalStateException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Navegação de cena
    // ════════════════════════════════════════════════════════════════════════

    private void launchGame(GameConfig config) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/GameView.fxml"));
        Parent root = loader.load();

        // Repassa a configuração para o GameController ANTES de exibir a cena
        GameController gameController = loader.getController();
        gameController.setup(config);

        Stage stage = (Stage) startButton.getScene().getWindow();

        // Transição suave de tela
        FadeTransition fade = new FadeTransition(Duration.millis(250), root);
        fade.setFromValue(0);
        fade.setToValue(1);

        stage.setScene(new Scene(root));
        stage.setTitle("Xadrez — " + config.getMode().getDisplayName());
        stage.show();
        fade.play();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════════════

    private GameMode getSelectedMode() {
        if (pveRadio.isSelected()) return GameMode.PVE;
        if (eveRadio.isSelected()) return GameMode.EVE;
        return GameMode.PVP;
    }

    private void updateDifficultyDesc(BotDifficulty diff) {
        if (difficultyDescLabel == null || diff == null) return;
        difficultyDescLabel.setText(switch (diff) {
            case BEGINNER -> "Perfeito para aprender. Comete erros frequentes.";
            case EASY     -> "Joga razoavelmente, mas erra em táticas simples.";
            case MEDIUM   -> "Jogo equilibrado. Bom desafio para jogadores casuais.";
            case HARD     -> "Tática sólida. Difícil de vencer sem experiência.";
            case EXPERT   -> "Jogo quase perfeito. Apenas para jogadores avançados.";
            case MAXIMUM  -> "Stockfish sem restrições. Praticamente imbatível.";
        });
    }

    private int parsePositiveInt(String raw, String fieldName) {
        int n = parseNonNegativeInt(raw, fieldName);
        if (n <= 0) throw new ValidationException(fieldName + " deve ser maior que zero.");
        return n;
    }

    private int parseNonNegativeInt(String raw, String fieldName) {
        try {
            int n = Integer.parseInt(raw.strip());
            if (n < 0) throw new ValidationException(fieldName + " não pode ser negativo.");
            return n;
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + ": insira um número inteiro válido.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        FadeTransition ft = new FadeTransition(Duration.millis(150), errorLabel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void clearError() {
        errorLabel.setText("");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Tipos internos
    // ════════════════════════════════════════════════════════════════════════

    /** Exceção de validação leve para uso interno neste controlador. */
    private static class ValidationException extends RuntimeException {
        ValidationException(String msg) { super(msg); }
    }

    /** Célula que exibe apenas o {@code displayName} do enum. */
    private static class DifficultyCell extends ListCell<BotDifficulty> {
        @Override
        protected void updateItem(BotDifficulty item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.getDisplayName());
        }
    }
}