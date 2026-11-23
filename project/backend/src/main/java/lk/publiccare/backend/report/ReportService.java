package lk.publiccare.backend.report;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lk.publiccare.backend.user.PublicCareUser;
import lk.publiccare.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class ReportService {
    private final ReportRepository reports;
    private final UserRepository users;
    @PersistenceContext private EntityManager entityManager;

    public ReportService(ReportRepository reports, UserRepository users) {
        this.reports = reports;
        this.users = users;
    }

    @Transactional
    public ReportDtos.ReportResponse create(Long ownerId, ReportDtos.CreateRequest request) {
        PublicCareUser owner = users.findById(ownerId).orElseThrow(ReportNotFoundException::new);
        PublicCareReport report = reports.saveAndFlush(new PublicCareReport(clean(request.title()), clean(request.description()),
                clean(request.category()), clean(request.location()), owner));
        // Read once more so the generated reference is loaded after INSERT.
        return response(reports.findById(report.getId()).orElseThrow(ReportNotFoundException::new));
    }

    @Transactional(readOnly = true)
    public ReportDtos.PageResponse<ReportDtos.ReportResponse> mine(Long ownerId, int page, int size, String sort,
            String direction, String status, String category, String query) {
        return list(ownerId, page, size, sort, direction, status, category, query);
    }

    @Transactional(readOnly = true)
    public ReportDtos.PageResponse<AdminReportDtos.ReportResponse> adminList(int page, int size, String sort,
            String direction, String status, String category, String query) {
        Specification<PublicCareReport> criteria = criteria(null, status, category, query);
        Page<PublicCareReport> result = findPage(criteria, page, size, sort, direction);
        return new ReportDtos.PageResponse<>(result.getContent().stream().map(ReportService::adminResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(), summarize(criteria));
    }

    private ReportDtos.PageResponse<ReportDtos.ReportResponse> list(Long ownerId, int page, int size, String sort,
            String direction, String status, String category, String query) {
        Specification<PublicCareReport> criteria = criteria(ownerId, status, category, query);
        Page<PublicCareReport> result = findPage(criteria, page, size, sort, direction);
        ReportDtos.Summary summary = summarize(criteria);
        return new ReportDtos.PageResponse<>(result.getContent().stream().map(ReportService::response).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(), summary);
    }

    private Page<PublicCareReport> findPage(Specification<PublicCareReport> criteria, int page, int size,
            String sort, String direction) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        String sortProperty = switch (sort == null ? "createdAt" : sort) {
            case "createdAt" -> "createdAt";
            case "updatedAt" -> "updatedAt";
            case "title" -> "title";
            case "status" -> "status";
            case "category" -> "category";
            default -> throw new IllegalArgumentException("Unsupported sort field.");
        };
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return reports.findAll(criteria, PageRequest.of(page, size, Sort.by(sortDirection, sortProperty)));
    }

    @Transactional(readOnly = true)
    public ReportDtos.Summary adminSummary(String status, String category, String query) {
        return summarize(criteria(null, status, category, query));
    }

    @Transactional(readOnly = true)
    public ReportDtos.ReportResponse mineDetail(Long ownerId, Long id) {
        return response(reports.findOne(ownerAndId(ownerId, id)).orElseThrow(ReportNotFoundException::new));
    }

    @Transactional(readOnly = true)
    public AdminReportDtos.ReportResponse adminDetail(Long id) {
        return adminResponse(reports.findById(id).orElseThrow(ReportNotFoundException::new));
    }

    @Transactional
    public ReportDtos.ReportResponse updateMine(Long ownerId, Long id, ReportDtos.UpdateRequest request) {
        PublicCareReport report = reports.findOne(ownerAndId(ownerId, id))
                .orElseThrow(ReportNotFoundException::new);
        report.updateDetails(clean(request.title()), clean(request.description()), clean(request.category()), clean(request.location()));
        report.revokePublicApproval();
        return response(reports.save(report));
    }

    @Transactional
    public AdminReportDtos.ReportResponse updateStatus(Long id, ReportDtos.StatusRequest request) {
        if (request.status() == null) throw new IllegalArgumentException("A valid status is required.");
        PublicCareReport report = reports.findById(id).orElseThrow(ReportNotFoundException::new);
        if (report.getStatus() != request.status()) report.revokePublicApproval();
        report.setStatus(request.status());
        report.setAdminUpdateNote(request.adminUpdateNote() == null ? null : clean(request.adminUpdateNote()));
        return adminResponse(reports.save(report));
    }

    @Transactional
    public boolean setPublicApproval(Long id, boolean approved, Long adminId) {
        PublicCareReport report = reports.findById(id).orElseThrow(ReportNotFoundException::new);
        if (approved) report.approveForPublic(adminId);
        else report.revokePublicApproval();
        reports.save(report);
        return report.isPublicApproved();
    }

    private ReportDtos.Summary summarize(Specification<PublicCareReport> criteria) {
        Map<String, Long> categories = new LinkedHashMap<>();
        var builder = entityManager.getCriteriaBuilder();
        var groupedQuery = builder.createQuery(Object[].class);
        var reportRoot = groupedQuery.from(PublicCareReport.class);
        groupedQuery.multiselect(reportRoot.get("category"), builder.count(reportRoot)).where(criteria.toPredicate(reportRoot, groupedQuery, builder)).groupBy(reportRoot.get("category")).orderBy(builder.asc(reportRoot.get("category")));
        entityManager.createQuery(groupedQuery).getResultList().forEach(row -> categories.put((String) row[0], (Long) row[1]));
        long total = reports.count(criteria);
        long pending = reports.count(criteria.and((root, query, cb) -> cb.equal(root.get("status"), ReportStatus.PENDING)));
        long inProgress = reports.count(criteria.and((root, query, cb) -> cb.equal(root.get("status"), ReportStatus.IN_PROGRESS)));
        long resolved = reports.count(criteria.and((root, query, cb) -> cb.equal(root.get("status"), ReportStatus.RESOLVED)));
        return new ReportDtos.Summary(total, pending, inProgress, resolved, categories);
    }

    static Specification<PublicCareReport> ownerAndId(Long ownerId, Long id) {
        return criteria(ownerId, null, null, null).and((root, query, cb) -> cb.equal(root.get("id"), id));
    }

    private static Specification<PublicCareReport> criteria(Long ownerId, String status, String category, String query) {
        return (root, cq, cb) -> {
            var predicate = cb.conjunction();
            if (ownerId != null) predicate = cb.and(predicate, cb.equal(root.get("owner").get("id"), ownerId));
            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                ReportStatus parsed;
                try { parsed = ReportStatus.valueOf(status.trim().toUpperCase(Locale.ROOT).replace(' ', '_')); }
                catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Unsupported report status."); }
                predicate = cb.and(predicate, cb.equal(root.get("status"), parsed));
            }
            if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) predicate = cb.and(predicate, cb.equal(root.get("category"), category));
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicate = cb.and(predicate, cb.or(cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("reference")), pattern), cb.like(cb.lower(root.get("location")), pattern)));
            }
            return predicate;
        };
    }

    private static String clean(String value) { return value.trim(); }
    private static ReportDtos.ReportResponse response(PublicCareReport report) {
        String reference = report.getReference() == null ? "PC-" + String.format(Locale.ROOT, "%08d", report.getId()) : report.getReference();
        return new ReportDtos.ReportResponse(report.getId(), reference, report.getTitle(), report.getDescription(),
                report.getCategory(), report.getLocation(), report.getStatus(), report.getAdminUpdateNote(),
                report.getOwner().getId(), report.getOwner().getFullName(), report.getCreatedAt(), report.getUpdatedAt());
    }

    private static AdminReportDtos.ReportResponse adminResponse(PublicCareReport report) {
        String reference = report.getReference() == null ? "PC-" + String.format(Locale.ROOT, "%08d", report.getId()) : report.getReference();
        return new AdminReportDtos.ReportResponse(report.getId(), reference, report.getTitle(), report.getDescription(),
                report.getCategory(), report.getLocation(), report.getStatus(), report.getAdminUpdateNote(),
                report.getOwner().getId(), report.getOwner().getFullName(), report.getCreatedAt(), report.getUpdatedAt(),
                report.isPublicApproved());
    }
}
