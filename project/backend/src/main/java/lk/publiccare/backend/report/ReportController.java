package lk.publiccare.backend.report;

import jakarta.validation.Valid;
import lk.publiccare.backend.security.PublicCarePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService service;
    private final ReportImageService imageService;
    private final ReportProgressService progressService;
    public ReportController(ReportService service, ReportImageService imageService, ReportProgressService progressService) { this.service = service; this.imageService = imageService; this.progressService = progressService; }

    @GetMapping("/{id}/progress-updates")
    public java.util.List<ReportDtos.ProgressUpdateResponse> progress(@PathVariable Long id, Authentication authentication) { return progressService.citizenList(principal(authentication).getUserId(), id); }
    @GetMapping("/{id}/progress-updates/{updateId}/image")
    public ResponseEntity<byte[]> progressImage(@PathVariable Long id, @PathVariable Long updateId, Authentication authentication) { return imageResponse(progressService.citizenImage(principal(authentication).getUserId(), id, updateId)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportDtos.ReportResponse create(@Valid @RequestBody ReportDtos.CreateRequest request, Authentication authentication) {
        return service.create(principal(authentication).getUserId(), request);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadImage(@PathVariable Long id, @RequestPart("image") MultipartFile image, Authentication authentication) {
        imageService.upload(principal(authentication).getUserId(), id, image);
    }

    @GetMapping(value = "/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable Long id, Authentication authentication) {
        return imageResponse(imageService.citizenImage(principal(authentication).getUserId(), id));
    }

    static ResponseEntity<byte[]> imageResponse(ReportImageService.ImageContent image) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.bytes().length)
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(image.bytes());
    }

    @GetMapping("/mine")
    public ReportDtos.PageResponse<ReportDtos.ReportResponse> mine(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction, @RequestParam(required = false) String status,
            @RequestParam(required = false) String category, @RequestParam(required = false, name = "q") String query,
            Authentication authentication) {
        return service.mine(principal(authentication).getUserId(), page, size, sort, direction, status, category, query);
    }

    @GetMapping("/{id}")
    public ReportDtos.ReportResponse detail(@PathVariable Long id, Authentication authentication) {
        return service.mineDetail(principal(authentication).getUserId(), id);
    }

    @PutMapping("/{id}")
    public ReportDtos.ReportResponse update(@PathVariable Long id, @Valid @RequestBody ReportDtos.UpdateRequest request,
            Authentication authentication) {
        return service.updateMine(principal(authentication).getUserId(), id, request);
    }

    private static PublicCarePrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof PublicCarePrincipal principal)) {
            throw new IllegalStateException("Authenticated database principal is unavailable.");
        }
        return principal;
    }
}
