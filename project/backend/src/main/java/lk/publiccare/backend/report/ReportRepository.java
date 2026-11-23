package lk.publiccare.backend.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReportRepository extends JpaRepository<PublicCareReport, Long>, JpaSpecificationExecutor<PublicCareReport> {
    java.util.Optional<PublicCareReport> findByIdAndPublicApprovedTrue(Long id);
}
