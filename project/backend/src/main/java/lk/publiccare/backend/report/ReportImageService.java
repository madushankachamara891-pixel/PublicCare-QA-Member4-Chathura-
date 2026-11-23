package lk.publiccare.backend.report;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.ImageIO;
import lk.publiccare.backend.user.PublicCareUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReportImageService {
    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 10_000_000L;

    private final ReportRepository reports;
    private final ReportImageRepository images;
    private final PrivateImageStorage storage;

    public ReportImageService(ReportRepository reports, ReportImageRepository images, PrivateImageStorage storage) {
        this.reports = reports;
        this.images = images;
        this.storage = storage;
    }

    @Transactional
    public void upload(Long ownerId, Long reportId, MultipartFile file) {
        PublicCareReport report = reports.findOne(ReportService.ownerAndId(ownerId, reportId))
                .orElseThrow(ReportNotFoundException::new);
        if (images.existsByReportId(reportId)) throw new ImageAlreadyExistsException();
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Choose an image to upload.");
        if (file.getSize() > MAX_IMAGE_BYTES) throw new IllegalArgumentException("Image must be 5 MB or smaller.");

        byte[] content;
        try { content = file.getBytes(); }
        catch (IOException exception) { throw new IllegalArgumentException("Unable to read uploaded image."); }
        if (content.length == 0 || content.length > MAX_IMAGE_BYTES) throw new IllegalArgumentException("Image must be between 1 byte and 5 MB.");

        DetectedImage detected = validateImage(content);
        String key = storage.store(content, detected.extension());
        try {
            images.saveAndFlush(new ReportImage(report, key, detected.contentType(), content.length));
        } catch (RuntimeException exception) {
            storage.delete(key);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ImageContent citizenImage(Long ownerId, Long reportId) {
        reports.findOne(ReportService.ownerAndId(ownerId, reportId)).orElseThrow(ReportNotFoundException::new);
        return readImage(reportId);
    }

    @Transactional(readOnly = true)
    public ImageContent adminImage(Long reportId) {
        if (!reports.existsById(reportId)) throw new ReportNotFoundException();
        return readImage(reportId);
    }

    private ImageContent readImage(Long reportId) {
        ReportImage image = images.findByReportId(reportId).orElseThrow(ReportNotFoundException::new);
        return new ImageContent(storage.read(image.getStorageKey()), image.getContentType());
    }

    private static DetectedImage validateImage(byte[] bytes) {
        if (startsWith(bytes, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff})) {
            validateDecodedImage(bytes, "JPG");
            return new DetectedImage("image/jpeg", "jpg");
        }
        if (startsWith(bytes, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})) {
            validateDecodedImage(bytes, "PNG");
            return new DetectedImage("image/png", "png");
        }
        if (isWebp(bytes)) return new DetectedImage("image/webp", "webp");
        throw new IllegalArgumentException("Only valid JPG, PNG, or WebP images are supported.");
    }

    private static void validateDecodedImage(byte[] bytes, String label) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) throw new IllegalArgumentException("Uploaded file is not a valid " + label + " image.");
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Uploaded file is not a valid " + label + " image.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException("Image dimensions must not exceed 10 megapixels.");
                }
                if (reader.read(0) == null) throw new IllegalArgumentException("Uploaded file is not a valid " + label + " image.");
            } finally { reader.dispose(); }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Uploaded file is not a valid " + label + " image.");
        }
    }

    private static boolean isWebp(byte[] bytes) {
        if (bytes.length < 20 || !ascii(bytes, 0, "RIFF") || !ascii(bytes, 8, "WEBP")) return false;
        long riffLength = Integer.toUnsignedLong((bytes[4] & 0xff) | ((bytes[5] & 0xff) << 8)
                | ((bytes[6] & 0xff) << 16) | (bytes[7] << 24));
        if (riffLength != bytes.length - 8L) return false;
        int offset = 12;
        boolean imageChunk = false;
        while (offset + 8 <= bytes.length) {
            String chunk = new String(bytes, offset, 4, java.nio.charset.StandardCharsets.US_ASCII);
            long chunkLength = Integer.toUnsignedLong((bytes[offset + 4] & 0xff) | ((bytes[offset + 5] & 0xff) << 8)
                    | ((bytes[offset + 6] & 0xff) << 16) | (bytes[offset + 7] << 24));
            long next = offset + 8L + chunkLength + (chunkLength & 1L);
            if (next > bytes.length) return false;
            int payload = offset + 8;
            if ("VP8 ".equals(chunk) && chunkLength >= 10 && bytes[payload + 3] == (byte) 0x9d
                    && bytes[payload + 4] == 0x01 && bytes[payload + 5] == 0x2a) {
                int width = ((bytes[payload + 7] & 0x3f) << 8) | (bytes[payload + 6] & 0xff);
                int height = ((bytes[payload + 9] & 0x3f) << 8) | (bytes[payload + 8] & 0xff);
                imageChunk = dimensionsAllowed(width, height);
            } else if ("VP8L".equals(chunk) && chunkLength >= 5 && bytes[payload] == 0x2f) {
                int width = 1 + (bytes[payload + 1] & 0xff) + ((bytes[payload + 2] & 0x3f) << 8);
                int height = 1 + ((bytes[payload + 2] & 0xc0) >>> 6) + ((bytes[payload + 3] & 0xff) << 2)
                        + ((bytes[payload + 4] & 0x0f) << 10);
                imageChunk = dimensionsAllowed(width, height);
            } else if ("VP8X".equals(chunk) && chunkLength == 10) {
                int width = 1 + (bytes[payload + 4] & 0xff) + ((bytes[payload + 5] & 0xff) << 8)
                        + ((bytes[payload + 6] & 0xff) << 16);
                int height = 1 + (bytes[payload + 7] & 0xff) + ((bytes[payload + 8] & 0xff) << 8)
                        + ((bytes[payload + 9] & 0xff) << 16);
                if (!dimensionsAllowed(width, height)) return false;
            }
            offset = (int) next;
        }
        return imageChunk && offset == bytes.length;
    }

    private static boolean dimensionsAllowed(int width, int height) {
        return width > 0 && height > 0 && (long) width * height <= MAX_IMAGE_PIXELS;
    }

    private static boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) if (bytes[i] != signature[i]) return false;
        return true;
    }

    private static boolean ascii(byte[] bytes, int offset, String value) {
        return new String(bytes, offset, value.length(), java.nio.charset.StandardCharsets.US_ASCII).equals(value);
    }

    private record DetectedImage(String contentType, String extension) { }
    public record ImageContent(byte[] bytes, String contentType) { }

    public static class ImageAlreadyExistsException extends RuntimeException { }
}
