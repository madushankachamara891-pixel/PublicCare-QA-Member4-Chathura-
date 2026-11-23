package lk.publiccare.backend.report;

import java.time.Instant;

/** Approval state is returned only from admin report endpoints. */
public final class AdminReportDtos {
    private AdminReportDtos() { }

    public record ReportResponse(Long id, String reference, String title, String description,
                                 String category, String location, ReportStatus status,
                                 String adminUpdateNote, Long ownerId, String ownerName,
                                 Instant createdAt, Instant updatedAt, boolean publicApproved) { }

    public record ProgressUpdateResponse(Long id, ReportStatus status, String note,
                                         Instant createdAt, boolean hasImage,
                                         boolean publicApproved, boolean notePublicApproved) { }
}
