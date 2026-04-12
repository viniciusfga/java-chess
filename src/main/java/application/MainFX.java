package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        URL fxml = getClass().getResource("/gui/GameView.fxml");
        System.out.println(fxml);

        FXMLLoader loader = new FXMLLoader(fxml);
        // Aumente a largura (width) de 900 para 1100 ou 1200
        Scene scene = new Scene(loader.load(), 1000, 750);

        // IMPORTANTE: Altere para true para você poder ajustar manualmente e testar o tamanho ideal
        stage.setResizable(true);

        URL css = getClass().getResource("/gui/style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("Sistema de Xadrez");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}