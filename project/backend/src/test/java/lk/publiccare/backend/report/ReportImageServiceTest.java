package lk.publiccare.backend.report;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ReportImageServiceTest {
    private static final byte[] PNG_1PX = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p5sAAAAASUVORK5CYII=");

    @Mock ReportRepository reports;
    @Mock ReportImageRepository images;
    @Mock PrivateImageStorage storage;
    @Mock PublicCareReport report;
    ReportImageService service;

    @BeforeEach void setUp() {
        service = new ReportImageService(reports, images, storage);
    }

    @Test
    void acceptsOneValidPngAndStoresOnlyTheGeneratedStorageKeyInMetadata() {
        ownedReportWithoutImage();
        when(storage.store(PNG_1PX, "png")).thenReturn("123e4567-e89b-12d3-a456-426614174000.png");

        service.upload(42L, 51L, new MockMultipartFile("image", "citizen-name.png", "text/plain", PNG_1PX));

        verify(storage).store(PNG_1PX, "png");
        verify(images).saveAndFlush(any(ReportImage.class));
    }

    @Test
    void rejectsContentThatOnlyClaimsToBeAnImage() {
        ownedReportWithoutImage();
        assertThrows(IllegalArgumentException.class, () -> service.upload(42L, 51L,
                new MockMultipartFile("image", "fake.png", "image/png", "not an image".getBytes())));
        verify(storage, never()).store(any(), any());
        verify(images, never()).saveAndFlush(any());
    }

    @Test
    void rejectsImagesOverFiveMegabytes() {
        ownedReportWithoutImage();
        byte[] oversized = new byte[(int) ReportImageService.MAX_IMAGE_BYTES + 1];
        assertThrows(IllegalArgumentException.class, () -> service.upload(42L, 51L,
                new MockMultipartFile("image", "large.png", "image/png", oversized)));
        verify(storage, never()).store(any(), any());
    }

    @Test
    void nonOwnerCannotUploadAndImageIsNotWritten() {
        when(reports.findOne(any(Specification.class))).thenReturn(Optional.empty());
        assertThrows(ReportNotFoundException.class, () -> service.upload(73L, 51L,
                new MockMultipartFile("image", "photo.png", "image/png", PNG_1PX)));
        verify(storage, never()).store(any(), any());
        verify(images, never()).existsByReportId(51L);
    }

    @Test
    void duplicateAttachmentIsRejected() {
        when(reports.findOne(any(Specification.class))).thenReturn(Optional.of(report));
        when(images.existsByReportId(51L)).thenReturn(true);
        assertThrows(ReportImageService.ImageAlreadyExistsException.class, () -> service.upload(42L, 51L,
                new MockMultipartFile("image", "photo.png", "image/png", PNG_1PX)));
        verify(storage, never()).store(any(), any());
    }

    private void ownedReportWithoutImage() {
        when(reports.findOne(any(Specification.class))).thenReturn(Optional.of(report));
        when(images.existsByReportId(51L)).thenReturn(false);
    }
}
