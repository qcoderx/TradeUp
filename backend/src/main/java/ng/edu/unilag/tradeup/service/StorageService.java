package ng.edu.unilag.tradeup.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import ng.edu.unilag.tradeup.web.error.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Saves listing photos to disk and hands back the URL they will be served from.
 *
 * <p>Uploads are checked on content type, extension and magic bytes rather than
 * on the filename alone, and are always stored under a freshly generated name.
 * A student cannot choose where their file lands or what it is called, which
 * closes off both path traversal and overwriting someone elses photo.
 */
@Service
public class StorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");
    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/jpeg", "image/png", "image/webp");

    private final Path uploadDirectory;
    private final String publicPrefix;

    public StorageService(
            @Value("${tradeup.storage.upload-dir}") Path uploadDirectory,
            @Value("${tradeup.storage.public-prefix:/uploads}") String publicPrefix) {
        this.uploadDirectory = uploadDirectory;
        this.publicPrefix = publicPrefix;
    }

    /**
     * Stores one uploaded image.
     *
     * @return the public URL path, e.g. {@code /uploads/6f2c....jpg}
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Choose an image to upload.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ValidationException("That image is larger than 5 MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ValidationException("Upload a JPG, PNG or WebP image.");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ValidationException("Upload a JPG, PNG or WebP image.");
        }

        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(uploadDirectory);

            // The name is ours, never the uploaders, so the path cannot be steered.
            String storedName = UUID.randomUUID() + "." + extension;
            Path target = uploadDirectory.resolve(storedName);

            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);

            if (!looksLikeAnImage(target)) {
                Files.deleteIfExists(target);
                throw new ValidationException("That file is not a real image.");
            }

            return publicPrefix + "/" + storedName;

        } catch (IOException ex) {
            throw new UncheckedIOException("Could not save the uploaded image.", ex);
        }
    }

    /**
     * Confirms the first bytes match a format we accept, so renaming a .exe to
     * .png does not get it past the extension check.
     */
    private boolean looksLikeAnImage(Path path) throws IOException {
        byte[] header = new byte[12];
        try (InputStream in = Files.newInputStream(path)) {
            if (in.read(header) < 12) {
                return false;
            }
        }
        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47
        if ((header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G') {
            return true;
        }
        // WebP: "RIFF" .... "WEBP"
        return header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P';
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
