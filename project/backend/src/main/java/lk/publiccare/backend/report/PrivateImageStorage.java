package lk.publiccare.backend.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PrivateImageStorage {
    private final Path root;

    public PrivateImageStorage(@Value("${publiccare.upload.directory:${user.home}/.publiccare/uploads}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    public String store(byte[] content, String extension) {
        String key = UUID.randomUUID() + "." + extension;
        Path destination = resolve(key);
        try {
            Files.createDirectories(root);
            Files.write(destination, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return key;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store report image.", exception);
        }
    }

    public byte[] read(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read report image.", exception);
        }
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to remove report image.", exception);
        }
    }

    private Path resolve(String key) {
        if (key == null || !key.matches("[0-9a-fA-F-]{36}\\.(jpg|png|webp)")) {
            throw new IllegalArgumentException("Invalid image storage key.");
        }
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("Invalid image storage key.");
        return resolved;
    }
}
