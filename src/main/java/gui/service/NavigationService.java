package gui.service;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import java.io.IOException;

public class NavigationService {

    public void navigateToSetup(Pane currentRootPane) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/GameSetupView.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) currentRootPane.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Sistema de Xadrez - Configuração");
        stage.setResizable(false);
        stage.show();
    }
}