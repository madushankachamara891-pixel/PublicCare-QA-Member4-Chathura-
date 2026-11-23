package lk.publiccare.backend.report;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import lk.publiccare.backend.security.PublicCarePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {
    private static final Logger logger = LoggerFactory.getLogger(AdminReportController.class);
    private final ReportService service;
    private final ReportImageService imageService;
    private final ReportProgressService progressService;
    private final ReportDeletionService deletionService;
    public AdminReportController(ReportService service, ReportImageService imageService, ReportProgressService progressService,
            ReportDeletionService deletionService) { this.service = service; this.imageService = imageService; this.progressService = progressService; this.deletionService = deletionService; }

    @GetMapping("/{id}/progress-updates")
    public java.util.List<AdminReportDtos.ProgressUpdateResponse> progress(@PathVariable Long id) { return progressService.adminList(id); }
    @GetMapping("/{id}/progress-updates/{updateId}/image")
    public ResponseEntity<byte[]> progressImage(@PathVariable Long id, @PathVariable Long updateId) { return ReportController.imageResponse(progressService.adminImage(id, updateId)); }
    @org.springframework.web.bind.annotation.PostMapping(value = "/{id}/progress-updates", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public AdminReportDtos.ProgressUpdateResponse addProgress(@PathVariable Long id, @org.springframework.web.bind.annotation.RequestParam ReportStatus status,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String note,
            @org.springframework.web.bind.annotation.RequestPart(value = "image", required = false) org.springframework.web.multipart.MultipartFile image) {
        return progressService.create(id, status, note, image);
    }

    @GetMapping
    public ReportDtos.PageResponse<AdminReportDtos.ReportResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction, @RequestParam(required = false) String status,
            @RequestParam(required = false) String category, @RequestParam(required = false, name = "q") String query) {
        return service.adminList(page, size, sort, direction, status, category, query);
    }

    @GetMapping("/summary")
    public ReportDtos.Summary summary(@RequestParam(required = false) String status,
            @RequestParam(required = false) String category, @RequestParam(required = false, name = "q") String query) {
        return service.adminSummary(status, category, query);
    }

    @GetMapping("/{id}")
    public AdminReportDtos.ReportResponse detail(@PathVariable Long id) { return service.adminDetail(id); }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        try {
            deletionService.deleteAdmin(id);
        } catch (ReportNotFoundException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            logger.error("Admin report deletion failed for report {}.", id, exception);
            throw new ReportDeletionFailedException();
        }
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable Long id) {
        return ReportController.imageResponse(imageService.adminImage(id));
    }

    @PatchMapping("/{id}/status")
    public AdminReportDtos.ReportResponse updateStatus(@PathVariable Long id, @Valid @RequestBody ReportDtos.StatusRequest request) {
        return service.updateStatus(id, request);
    }

    @PatchMapping("/{id}/public-approval")
    public PublicReportDtos.ApprovalResponse approveReport(@PathVariable Long id,
            @RequestBody PublicReportDtos.ApprovalRequest request, Authentication authentication) {
        return new PublicReportDtos.ApprovalResponse(service.setPublicApproval(id, request.approved(), adminId(authentication)));
    }

    @PatchMapping("/{id}/progress-updates/{updateId}/public-approval")
    public PublicReportDtos.ApprovalResponse approveProgressUpdate(@PathVariable Long id, @PathVariable Long updateId,
            @RequestBody PublicReportDtos.ApprovalRequest request, Authentication authentication) {
        return new PublicReportDtos.ApprovalResponse(progressService.setPublicApproval(id, updateId, request.approved(), adminId(authentication)));
    }

    @PatchMapping("/{id}/progress-updates/{updateId}/note-public-approval")
    public PublicReportDtos.ApprovalResponse approveProgressNote(@PathVariable Long id, @PathVariable Long updateId,
            @RequestBody PublicReportDtos.ApprovalRequest request, Authentication authentication) {
        return new PublicReportDtos.ApprovalResponse(progressService.setNotePublicApproval(id, updateId, request.approved(), adminId(authentication)));
    }

    private static Long adminId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof PublicCarePrincipal principal)
                || authentication.getAuthorities().stream().noneMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            throw new AccessDeniedException("Administrator access required.");
        }
        return principal.getUserId();
    }
}
