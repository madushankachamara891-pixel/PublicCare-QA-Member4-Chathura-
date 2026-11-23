package lk.publiccare.backend.report;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/reports")
public class PublicReportController {
    private final PublicReportService service;

    public PublicReportController(PublicReportService service) {
        this.service = service;
    }

    @GetMapping
    public PublicReportDtos.Page list(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "9") int size) {
        return service.list(page, size);
    }

    @GetMapping("/{id}")
    public PublicReportDtos.Report detail(@PathVariable Long id) {
        return service.detail(id);
    }
}
