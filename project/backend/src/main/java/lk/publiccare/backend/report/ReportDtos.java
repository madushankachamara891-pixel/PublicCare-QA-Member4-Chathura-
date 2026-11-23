package lk.publiccare.backend.report;

import java.time.Instant;
import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ReportDtos {
    private ReportDtos() { }

    public record CreateRequest(@NotBlank @Size(max = 160) String title,
                                @NotBlank @Size(max = 5000) String description,
                                @NotBlank @Size(max = 80) String category,
                                @NotBlank @Size(max = 240) String location) { }
    public record UpdateRequest(@NotBlank @Size(max = 160) String title,
                                @NotBlank @Size(max = 5000) String description,
                                @NotBlank @Size(max = 80) String category,
                                @NotBlank @Size(max = 240) String location) { }
    public record StatusRequest(ReportStatus status, @Size(max = 1000) String adminUpdateNote) { }
    public record ProgressUpdateResponse(Long id, ReportStatus status, String note, Instant createdAt, boolean hasImage) { }
    public record ReportResponse(Long id, String reference, String title, String description,
                                 String category, String location, ReportStatus status,
                                 String adminUpdateNote, Long ownerId, String ownerName,
                                 Instant createdAt, Instant updatedAt) { }
    public record Summary(long total, long pending, long inProgress, long resolved, Map<String, Long> categories) { }
    public record PageResponse<T>(java.util.List<T> content, int page, int size, long totalElements,
                                  int totalPages, Summary summary) { }
    public record ErrorResponse(String error) { }
}
