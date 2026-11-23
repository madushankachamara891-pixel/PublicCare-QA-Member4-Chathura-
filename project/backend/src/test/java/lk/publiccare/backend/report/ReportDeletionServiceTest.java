package lk.publiccare.backend.report;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import lk.publiccare.backend.user.PublicCareUser;
import lk.publiccare.backend.user.UserRole;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ReportDeletionServiceTest {
    @Mock ReportRepository reports;
    @Mock ReportImageRepository images;
    @Mock ReportProgressUpdateRepository progressUpdates;
    @Mock PrivateImageStorage storage;

    @TempDir Path imageDirectory;

    private ReportDeletionService service;

    @BeforeEach
    void setUp() {
        service = new ReportDeletionService(reports, images, progressUpdates, storage);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletesReportAndCleansAllStoredImagesOnlyAfterCommit() {
        PublicCareReport report = mock(PublicCareReport.class);
        ReportImage reportImage = mock(ReportImage.class);
        ReportProgressUpdate updateWithImage = mock(ReportProgressUpdate.class);
        ReportProgressUpdate updateWithoutImage = mock(ReportProgressUpdate.class);
        when(reports.findById(51L)).thenReturn(Optional.of(report));
        when(images.findByReportId(51L)).thenReturn(Optional.of(reportImage));
        when(reportImage.getStorageKey()).thenReturn("11111111-1111-1111-1111-111111111111.png");
        when(progressUpdates.findByReportIdOrderByCreatedAtAscIdAsc(51L))
                .thenReturn(List.of(updateWithImage, updateWithoutImage));
        when(updateWithImage.getStorageKey()).thenReturn("22222222-2222-2222-2222-222222222222.jpg");
        when(updateWithoutImage.getStorageKey()).thenReturn(null);

        service.deleteAdmin(51L);

        verify(images).findByReportId(51L);
        verify(progressUpdates).findByReportIdOrderByCreatedAtAscIdAsc(51L);
        verify(reports).delete(report);
        verify(reports).flush();
        verifyNoInteractions(storage);

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        verify(storage).delete("11111111-1111-1111-1111-111111111111.png");
        verify(storage).delete("22222222-2222-2222-2222-222222222222.jpg");
        verify(storage, never()).delete(null);
    }

    @Test
    void deletesResolvedPubliclyApprovedReportWithCitizenAndProgressImagesAfterCommit() throws IOException {
        var owner = new PublicCareUser("Citizen", "citizen@example.test", "hash", UserRole.CITIZEN);
        var report = new PublicCareReport("Road damage", "Description", "Roads", "Main street", owner);
        report.setStatus(ReportStatus.RESOLVED);
        report.approveForPublic(7L);
        var reportImage = new ReportImage(report, "11111111-1111-1111-1111-111111111111.png", "image/png", 1);
        var progressImage = new ReportProgressUpdate(report, ReportStatus.RESOLVED, "Repair completed",
                "22222222-2222-2222-2222-222222222222.jpg", "image/jpeg", 1L);
        progressImage.approveForPublic(7L);
        org.junit.jupiter.api.Assertions.assertEquals(ReportStatus.RESOLVED, report.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(report.isPublicApproved());
        org.junit.jupiter.api.Assertions.assertTrue(progressImage.isPublicApproved());
        Files.createFile(imageDirectory.resolve(reportImage.getStorageKey()));
        Files.createFile(imageDirectory.resolve(progressImage.getStorageKey()));
        var fileStorage = new PrivateImageStorage(imageDirectory.toString());
        service = new ReportDeletionService(reports, images, progressUpdates, fileStorage);
        when(reports.findById(51L)).thenReturn(Optional.of(report));
        when(images.findByReportId(51L)).thenReturn(Optional.of(reportImage));
        when(progressUpdates.findByReportIdOrderByCreatedAtAscIdAsc(51L)).thenReturn(List.of(progressImage));

        service.deleteAdmin(51L);

        verify(reports).delete(report);
        verify(reports).flush();
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(imageDirectory.resolve(reportImage.getStorageKey())));
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(imageDirectory.resolve(progressImage.getStorageKey())));

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(imageDirectory.resolve(reportImage.getStorageKey())));
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(imageDirectory.resolve(progressImage.getStorageKey())));
    }

    @Test
    void missingReportDoesNotDeleteDatabaseRowsOrFiles() {
        when(reports.findById(51L)).thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class, () -> service.deleteAdmin(51L));

        verify(reports, never()).delete(org.mockito.ArgumentMatchers.any(PublicCareReport.class));
        verifyNoInteractions(images, progressUpdates, storage);
    }

    @Test
    void databaseDeleteFailureIsPropagatedWithoutCleaningImages() {
        PublicCareReport report = mock(PublicCareReport.class);
        when(reports.findById(51L)).thenReturn(Optional.of(report));
        when(images.findByReportId(51L)).thenReturn(Optional.empty());
        when(progressUpdates.findByReportIdOrderByCreatedAtAscIdAsc(51L)).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new IllegalStateException("database failure")).when(reports).flush();

        assertThrows(IllegalStateException.class, () -> service.deleteAdmin(51L));

        verify(reports).delete(report);
        verifyNoInteractions(storage);
    }
}
