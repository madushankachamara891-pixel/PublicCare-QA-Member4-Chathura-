package lk.publiccare.backend.report;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportImageRepository extends JpaRepository<ReportImage, Long> {
    Optional<ReportImage> findByReportId(Long reportId);
    boolean existsByReportId(Long reportId);
}
