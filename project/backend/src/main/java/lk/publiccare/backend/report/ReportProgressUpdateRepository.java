package lk.publiccare.backend.report;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportProgressUpdateRepository extends JpaRepository<ReportProgressUpdate, Long> {
    List<ReportProgressUpdate> findByReportIdOrderByCreatedAtAscIdAsc(Long reportId);
    List<ReportProgressUpdate> findTop10ByReportIdAndPublicApprovedTrueOrderByCreatedAtDescIdDesc(Long reportId);
    java.util.Optional<ReportProgressUpdate> findByIdAndReportId(Long id, Long reportId);
}
