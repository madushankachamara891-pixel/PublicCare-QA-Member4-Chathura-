package lk.publiccare.backend.report;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ReportProgressServiceTest {
    private ReportRepository reports;
    private ReportProgressUpdateRepository updates;
    private PrivateImageStorage storage;
    private ReportProgressService service;

    @BeforeEach void setup() {
        reports = mock(ReportRepository.class); updates = mock(ReportProgressUpdateRepository.class); storage = mock(PrivateImageStorage.class);
        service = new ReportProgressService(reports, updates, storage);
        when(reports.findById(9L)).thenReturn(Optional.of(mock(PublicCareReport.class)));
        when(updates.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test void savesEachStatusAndNoteAsASeparateRecordWithoutImage() {
        var first = service.create(9L, ReportStatus.IN_PROGRESS, "Crew assigned", null);
        var second = service.create(9L, ReportStatus.RESOLVED, "Repair completed", null);
        assertEquals(ReportStatus.IN_PROGRESS, first.status());
        assertEquals("Crew assigned", first.note());
        assertFalse(first.hasImage());
        assertEquals(ReportStatus.RESOLVED, second.status());
        assertEquals("Repair completed", second.note());
        verify(updates, times(2)).saveAndFlush(any(ReportProgressUpdate.class));
        verify(storage, never()).store(any(), anyString());
    }

    @Test void rejectsInvalidImageContentBeforeCreatingHistoryOrChangingReport() {
        var image = new MockMultipartFile("image", "bad.png", "image/png", "not an image".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.create(9L, ReportStatus.IN_PROGRESS, "Working", image));
        verify(updates, never()).saveAndFlush(any());
        verify(storage, never()).store(any(), anyString());
    }

    @Test void rejectsImagesLargerThanFiveMebibytes() {
        var tooLarge = new MockMultipartFile("image", "big.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]);
        assertThrows(IllegalArgumentException.class, () -> service.create(9L, ReportStatus.IN_PROGRESS, "Working", tooLarge));
        verify(updates, never()).saveAndFlush(any());
        verify(storage, never()).store(any(), anyString());
    }

    @Test void persistsValidatedOptionalImageReference() {
        // A known one-pixel PNG, decoded by ImageIO as well as signature checked.
        byte[] png = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/n9sAAAAASUVORK5CYII=");
        when(storage.store(png, "png")).thenReturn("00000000-0000-0000-0000-000000000001.png");
        var image = new MockMultipartFile("image", "proof.png", "image/png", png);
        var response = service.create(9L, ReportStatus.RESOLVED, "Finished", image);
        assertTrue(response.hasImage());
        var captor = org.mockito.ArgumentCaptor.forClass(ReportProgressUpdate.class);
        verify(updates).saveAndFlush(captor.capture());
        assertEquals("image/png", captor.getValue().getContentType());
        assertEquals("00000000-0000-0000-0000-000000000001.png", captor.getValue().getStorageKey());
    }

    @Test void citizenHistoryRequiresReportOwnership() {
        when(reports.findOne(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<PublicCareReport>>any())).thenReturn(Optional.empty());
        assertThrows(ReportNotFoundException.class, () -> service.citizenList(77L, 9L));
        verifyNoInteractions(updates);
    }

    @Test void progressAndNoteApprovalsAreSavedIndependently() {
        var report = mock(PublicCareReport.class);
        var update = new ReportProgressUpdate(report, ReportStatus.IN_PROGRESS, "Crew assigned", null, null, null);
        when(reports.existsById(9L)).thenReturn(true);
        when(updates.findByIdAndReportId(8L, 9L)).thenReturn(Optional.of(update));

        assertTrue(service.setPublicApproval(9L, 8L, true, 7L));
        assertFalse(update.isNotePublicApproved());
        assertTrue(service.setNotePublicApproval(9L, 8L, true, 7L));
        assertTrue(update.isPublicApproved());
        assertTrue(update.isNotePublicApproved());
        assertFalse(service.setPublicApproval(9L, 8L, false, 7L));
        assertTrue(update.isNotePublicApproved());
        verify(updates, times(3)).save(update);
    }

    @Test void adminHistoryReturnsSeparateApprovalFlags() {
        var update = new ReportProgressUpdate(mock(PublicCareReport.class), ReportStatus.IN_PROGRESS,
                "Crew assigned", null, null, null);
        update.approveForPublic(7L);
        when(reports.existsById(9L)).thenReturn(true);
        when(updates.findByReportIdOrderByCreatedAtAscIdAsc(9L)).thenReturn(java.util.List.of(update));

        var response = service.adminList(9L).get(0);

        assertTrue(response.publicApproved());
        assertFalse(response.notePublicApproved());
    }
}
