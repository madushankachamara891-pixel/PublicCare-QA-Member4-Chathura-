package lk.publiccare.backend.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lk.publiccare.backend.user.PublicCareUser;
import lk.publiccare.backend.user.UserRepository;
import lk.publiccare.backend.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock ReportRepository reports;
    @Mock UserRepository users;
    ReportService service;

    @BeforeEach void setUp() { service = new ReportService(reports, users); }

    @Test
    void createPersistsReportWithOwnerResolvedFromAuthenticatedDatabaseIdentity() {
        PublicCareUser owner = org.mockito.Mockito.mock(PublicCareUser.class);
        when(owner.getId()).thenReturn(17L);
        when(owner.getFullName()).thenReturn("Citizen");
        when(users.findById(17L)).thenReturn(Optional.of(owner));
        PublicCareReport stored = org.mockito.Mockito.mock(PublicCareReport.class);
        when(reports.saveAndFlush(any(PublicCareReport.class))).thenReturn(stored);
        when(stored.getId()).thenReturn(51L);
        when(reports.findById(51L)).thenReturn(Optional.of(stored));
        when(stored.getOwner()).thenReturn(owner);
        when(stored.getTitle()).thenReturn("Broken light");
        when(stored.getDescription()).thenReturn("The light is out.");
        when(stored.getCategory()).thenReturn("Streetlights");
        when(stored.getLocation()).thenReturn("Galle");
        when(stored.getStatus()).thenReturn(ReportStatus.PENDING);

        var response = service.create(17L, new ReportDtos.CreateRequest(" Broken light ", " The light is out. ", " Streetlights ", " Galle "));

        assertEquals(17L, response.ownerId());
        assertEquals("PC-00000051", response.reference());
        verify(reports).saveAndFlush(any(PublicCareReport.class));
    }

    @Test
    void citizenDetailLookupUsesOwnerScopedCriteriaAndReturnsSameNotFoundForOtherUsers() {
        when(reports.findOne(any(Specification.class))).thenReturn(Optional.empty());
        assertThrows(ReportNotFoundException.class, () -> service.mineDetail(22L, 51L));
        assertLookupFiltersByOwnerAndReportId(22L, 51L);
    }

    @Test
    void citizenUpdateLookupUsesOwnerScopedCriteriaAndNonOwnerCannotUpdate() {
        when(reports.findOne(any(Specification.class))).thenReturn(Optional.empty());
        assertThrows(ReportNotFoundException.class, () -> service.updateMine(73L, 51L,
                new ReportDtos.UpdateRequest("Changed title", "Changed description", "Road damage", "Galle")));

        assertLookupFiltersByOwnerAndReportId(73L, 51L);
        verify(reports, never()).save(any(PublicCareReport.class));
    }

    private void assertLookupFiltersByOwnerAndReportId(Long ownerId, Long reportId) {
        ArgumentCaptor<Specification<PublicCareReport>> specification = ArgumentCaptor.forClass(Specification.class);
        verify(reports).findOne(specification.capture());

        Root<PublicCareReport> root = mock(Root.class);
        Path<Object> ownerPath = mock(Path.class);
        Path<Object> ownerIdPath = mock(Path.class);
        Path<Object> reportIdPath = mock(Path.class);
        when(root.get("owner")).thenReturn(ownerPath);
        when(ownerPath.get("id")).thenReturn(ownerIdPath);
        when(root.get("id")).thenReturn(reportIdPath);

        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);
        when(criteriaBuilder.conjunction()).thenReturn(predicate);
        specification.getValue().toPredicate(root, mock(CriteriaQuery.class), criteriaBuilder);

        verify(criteriaBuilder).equal(ownerIdPath, ownerId);
        verify(criteriaBuilder).equal(reportIdPath, reportId);
    }

    @Test
    void adminStatusUpdatePersistsAndReturnsTheUpdateNote() {
        PublicCareReport report = org.mockito.Mockito.mock(PublicCareReport.class);
        PublicCareUser owner = org.mockito.Mockito.mock(PublicCareUser.class);
        AtomicReference<String> storedNote = new AtomicReference<>();
        when(reports.findById(51L)).thenReturn(Optional.of(report));
        when(reports.save(report)).thenReturn(report);
        when(report.getId()).thenReturn(51L);
        when(report.getOwner()).thenReturn(owner);
        when(owner.getId()).thenReturn(17L);
        when(owner.getFullName()).thenReturn("Citizen");
        when(report.getTitle()).thenReturn("Broken bridge");
        when(report.getDescription()).thenReturn("The bridge is damaged.");
        when(report.getCategory()).thenReturn("Road damage");
        when(report.getLocation()).thenReturn("Galle");
        when(report.getStatus()).thenReturn(ReportStatus.RESOLVED);
        when(report.getAdminUpdateNote()).thenAnswer(invocation -> storedNote.get());
        doAnswer(invocation -> { storedNote.set(invocation.getArgument(0)); return null; })
                .when(report).setAdminUpdateNote(any());

        var response = service.updateStatus(51L,
                new ReportDtos.StatusRequest(ReportStatus.RESOLVED, "  The bridge has been repaired.  "));

        verify(report).setStatus(ReportStatus.RESOLVED);
        verify(report).setAdminUpdateNote("The bridge has been repaired.");
        verify(reports).save(report);
        assertEquals("The bridge has been repaired.", response.adminUpdateNote());
    }

    @Test
    void adminDetailIncludesPublicApprovalWithoutAddingItToCitizenDto() {
        PublicCareReport report = mock(PublicCareReport.class);
        PublicCareUser owner = mock(PublicCareUser.class);
        when(reports.findById(51L)).thenReturn(Optional.of(report));
        when(report.getId()).thenReturn(51L);
        when(report.getReference()).thenReturn("PC-00000051");
        when(report.getOwner()).thenReturn(owner);
        when(owner.getId()).thenReturn(17L);
        when(owner.getFullName()).thenReturn("Citizen");
        when(report.isPublicApproved()).thenReturn(true);

        AdminReportDtos.ReportResponse response = service.adminDetail(51L);

        assertTrue(response.publicApproved());
        verify(reports).findById(51L);
    }

    @Test
    void missingAuthenticatedOwnerCannotCreateReport() {
        when(users.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ReportNotFoundException.class, () -> service.create(99L,
                new ReportDtos.CreateRequest("A title", "A description", "Road damage", "A location")));
        verify(reports, never()).saveAndFlush(any(PublicCareReport.class));
    }
}
