package gui.audio;

import javafx.scene.media.AudioClip;
import java.net.URL;

public final class SoundManager {

    // Usamos caminhos relativos à raiz do "resources"
    private static final AudioClip MOVE = load("/assets/sounds/move.wav");
    private static final AudioClip CAPTURE = load("/assets/sounds/capture.wav");
    private static final AudioClip CHECK = load("/assets/sounds/check.wav");
    private static final AudioClip CHECKMATE = load("/assets/sounds/checkmate.wav");
    private static final AudioClip PROMOTE = load("/assets/sounds/promote.wav");

    private SoundManager() {}

    private static AudioClip load(String path) {
        try {
            // O getResource busca dentro do JAR/target, por isso não usa "src/main/resources"
            URL resource = SoundManager.class.getResource(path);

            if (resource == null) {
                System.err.println("⚠️ Alerta: Som não encontrado em: " + path);
                return null;
            }

            return new AudioClip(resource.toExternalForm());
        } catch (Exception e) {
            System.err.println("⚠️ Erro ao carregar áudio (" + path + "): " + e.getMessage());
            return null;
        }
    }

    public static void playMove() {
        if (MOVE != null) MOVE.play();
    }

    public static void playCapture() {
        if (CAPTURE != null) CAPTURE.play();
    }

    public static void playCheck() {
        if (CHECK != null) CHECK.play();
    }

    public static void playCheckmate() {
        if (CHECKMATE != null) CHECKMATE.play();
    }

    public static void playPromote() {
        if (PROMOTE != null) PROMOTE.play();
    }
}