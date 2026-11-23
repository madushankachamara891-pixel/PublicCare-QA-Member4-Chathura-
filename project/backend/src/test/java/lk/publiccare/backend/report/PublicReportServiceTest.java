package lk.publiccare.backend.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

class PublicReportServiceTest {
    private ReportRepository reports;
    private ReportProgressUpdateRepository updates;
    private PublicReportService service;

    @BeforeEach
    void setUp() {
        reports = mock(ReportRepository.class);
        updates = mock(ReportProgressUpdateRepository.class);
        service = new PublicReportService(reports, updates);
    }

    @Test
    void listQueriesOnlyApprovedReportsAndMapsOnlyPublicFields() {
        Instant createdAt = Instant.parse("2026-10-31T00:00:00Z");
        PublicCareReport report = mock(PublicCareReport.class);
        when(report.getId()).thenReturn(21L);
        when(report.getCategory()).thenReturn("Road damage");
        when(report.getStatus()).thenReturn(ReportStatus.IN_PROGRESS);
        when(report.getCreatedAt()).thenReturn(createdAt);
        when(reports.findAll(org.mockito.ArgumentMatchers.<Specification<PublicCareReport>>any(), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report), PageRequest.of(0, 9), 1));

        ReportProgressUpdate approvedNote = mock(ReportProgressUpdate.class);
        when(approvedNote.getStatus()).thenReturn(ReportStatus.IN_PROGRESS);
        when(approvedNote.getCreatedAt()).thenReturn(createdAt.plusSeconds(60));
        when(approvedNote.isNotePublicApproved()).thenReturn(true);
        when(approvedNote.getNote()).thenReturn("Crew assigned.");
        ReportProgressUpdate approvedUpdateWithPrivateNote = mock(ReportProgressUpdate.class);
        when(approvedUpdateWithPrivateNote.getStatus()).thenReturn(ReportStatus.PENDING);
        when(approvedUpdateWithPrivateNote.getCreatedAt()).thenReturn(createdAt.plusSeconds(30));
        when(approvedUpdateWithPrivateNote.isPublicApproved()).thenReturn(true);
        when(approvedUpdateWithPrivateNote.isNotePublicApproved()).thenReturn(false);
        when(approvedUpdateWithPrivateNote.getNote()).thenReturn("Internal contact details.");
        when(updates.findTop10ByReportIdAndPublicApprovedTrueOrderByCreatedAtDescIdDesc(21L))
                .thenReturn(List.of(approvedNote, approvedUpdateWithPrivateNote));

        PublicReportDtos.Page page = service.list(0, 9);

        assertEquals(1, page.totalElements());
        assertEquals("Road damage", page.content().get(0).category());
        assertEquals(ReportStatus.IN_PROGRESS, page.content().get(0).status());
        assertEquals(createdAt, page.content().get(0).reportedAt());
        assertEquals("Crew assigned.", page.content().get(0).progressUpdates().get(0).note());
        assertNull(page.content().get(0).progressUpdates().get(1).note());
        verify(reports).findAll(org.mockito.ArgumentMatchers.<Specification<PublicCareReport>>any(), org.mockito.ArgumentMatchers.any(Pageable.class));
        verify(updates).findTop10ByReportIdAndPublicApprovedTrueOrderByCreatedAtDescIdDesc(21L);
        verify(approvedNote, never()).getStorageKey();
        verify(approvedUpdateWithPrivateNote, never()).getStorageKey();
    }

    @Test
    void reportQuerySpecificationRequiresPublicApproval() {
        when(reports.findAll(org.mockito.ArgumentMatchers.<Specification<PublicCareReport>>any(), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 9), 0));

        service.list(0, 9);

        ArgumentCaptor<Specification<PublicCareReport>> specification = ArgumentCaptor.forClass(Specification.class);
        verify(reports).findAll(specification.capture(), org.mockito.ArgumentMatchers.any(Pageable.class));
        Root<PublicCareReport> root = mock(Root.class);
        Path<Boolean> approvedPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        when(root.<Boolean>get("publicApproved")).thenReturn(approvedPath);
        when(builder.isTrue(approvedPath)).thenReturn(predicate);

        assertEquals(predicate, specification.getValue().toPredicate(root, mock(CriteriaQuery.class), builder));
        verify(builder).isTrue(approvedPath);
    }

    @Test
    void rejectsOutOfRangePageRequests() {
        assertThrows(IllegalArgumentException.class, () -> service.list(-1, 9));
        assertThrows(IllegalArgumentException.class, () -> service.list(0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.list(0, 25));
    }
}
