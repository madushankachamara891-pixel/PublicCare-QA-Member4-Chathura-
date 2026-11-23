package lk.publiccare.backend.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lk.publiccare.backend.user.PublicCareUser;
import lk.publiccare.backend.user.UserRole;
import lk.publiccare.backend.user.UserRepository;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:report-deletion;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ReportDeletionService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReportDeletionJpaTest {
    private static final String REPORT_IMAGE_KEY = "11111111-1111-1111-1111-111111111111.png";
    private static final String PROGRESS_IMAGE_KEY = "22222222-2222-2222-2222-222222222222.jpg";

    @Autowired UserRepository users;
    @Autowired ReportRepository reports;
    @Autowired ReportImageRepository images;
    @Autowired ReportProgressUpdateRepository progressUpdates;
    @Autowired ReportDeletionService deletionService;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean PrivateImageStorage storage;

    @BeforeEach
    void allowGeneratedReferenceColumnInH2() {
        // PostgreSQL supplies this generated column; H2's Hibernate-created test schema does not.
        jdbc.execute("alter table reports alter column reference drop not null");
    }

    @Test
    void deletesResolvedApprovedReportWithBothImagesAndProgressHistory() {
        PublicCareUser owner = users.saveAndFlush(new PublicCareUser(
                "Citizen", "citizen@example.test", "hash", UserRole.CITIZEN));
        PublicCareReport report = reports.saveAndFlush(new PublicCareReport(
                "Road damage", "Description", "Roads", "Main street", owner));
        images.saveAndFlush(new ReportImage(report, REPORT_IMAGE_KEY, "image/png", 1));
        report.setStatus(ReportStatus.RESOLVED);
        report.approveForPublic(7L);
        reports.saveAndFlush(report);
        ReportProgressUpdate progress = new ReportProgressUpdate(report, ReportStatus.RESOLVED,
                "Repair completed", PROGRESS_IMAGE_KEY, "image/jpeg", 1L);
        progress.approveForPublic(7L);
        progressUpdates.saveAndFlush(progress);
        deletionService.deleteAdmin(report.getId());

        assertThat(reports.existsById(report.getId())).isFalse();
        assertThat(images.existsByReportId(report.getId())).isFalse();
        assertThat(progressUpdates.findByReportIdOrderByCreatedAtAscIdAsc(report.getId())).isEmpty();
        verify(storage).delete(REPORT_IMAGE_KEY);
        verify(storage).delete(PROGRESS_IMAGE_KEY);
    }
}
