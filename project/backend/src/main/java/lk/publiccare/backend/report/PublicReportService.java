package lk.publiccare.backend.report;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicReportService {
    private static final int MAX_PAGE_SIZE = 24;
    private static final int PUBLIC_UPDATES_PER_REPORT = 10;

    private final ReportRepository reports;
    private final ReportProgressUpdateRepository updates;

    public PublicReportService(ReportRepository reports, ReportProgressUpdateRepository updates) {
        this.reports = reports;
        this.updates = updates;
    }

    @Transactional(readOnly = true)
    public PublicReportDtos.Page list(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Specification<PublicCareReport> approved = (root, query, builder) -> builder.isTrue(root.get("publicApproved"));
        var result = reports.findAll(approved, pageable);
        List<PublicReportDtos.Report> content = result.getContent().stream().map(report -> {
            List<PublicReportDtos.ProgressUpdate> history = updates
                    .findTop10ByReportIdAndPublicApprovedTrueOrderByCreatedAtDescIdDesc(report.getId())
                    .stream()
                    .map(update -> new PublicReportDtos.ProgressUpdate(
                            update.getStatus(), update.getCreatedAt(),
                            update.isNotePublicApproved() ? update.getNote() : null))
                    .toList();
            return toPublicReport(report, history);
        }).toList();
        return new PublicReportDtos.Page(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PublicReportDtos.Report detail(Long id) {
        PublicCareReport report = reports.findByIdAndPublicApprovedTrue(id)
                .orElseThrow(ReportNotFoundException::new);
        List<PublicReportDtos.ProgressUpdate> history = updates
                .findTop10ByReportIdAndPublicApprovedTrueOrderByCreatedAtDescIdDesc(report.getId())
                .stream()
                .map(update -> new PublicReportDtos.ProgressUpdate(
                        update.getStatus(), update.getCreatedAt(),
                        update.isNotePublicApproved() ? update.getNote() : null))
                .toList();
        return toPublicReport(report, history);
    }

    private static PublicReportDtos.Report toPublicReport(PublicCareReport report,
            List<PublicReportDtos.ProgressUpdate> history) {
        return new PublicReportDtos.Report(report.getId(), report.getTitle(), report.getCategory(),
                report.getStatus(), report.getLocation(), report.getDescription(), report.getCreatedAt(), history);
    }
}
