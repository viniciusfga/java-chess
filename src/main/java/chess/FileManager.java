package chess;

import java.nio.file.Files;
import java.nio.file.Path;

public class FileManager {

    public static void savePGN(String content, String filename) {
        try {
            Files.writeString(Path.of(filename), content);
        } catch (Exception e) {
            System.out.println("Erro ao salvar arquivo: " + e.getMessage());
        }
    }
}