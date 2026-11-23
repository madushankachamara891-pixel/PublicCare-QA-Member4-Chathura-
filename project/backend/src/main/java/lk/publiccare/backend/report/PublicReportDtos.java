package lk.publiccare.backend.report;

import java.time.Instant;
import java.util.List;

public final class PublicReportDtos {
    private PublicReportDtos() { }

    public record Report(Long id, String title, String category, ReportStatus status, String location,
                         String description, Instant reportedAt,
                          List<ProgressUpdate> progressUpdates) { }
    public record ProgressUpdate(ReportStatus status, Instant createdAt, String note) { }
    public record Page(List<Report> content, int page, int size, long totalElements, int totalPages) { }
    public record ApprovalRequest(boolean approved) { }
    public record ApprovalResponse(boolean approved) { }
}
