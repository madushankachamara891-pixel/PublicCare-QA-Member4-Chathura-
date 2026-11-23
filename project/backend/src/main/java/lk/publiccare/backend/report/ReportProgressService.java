package lk.publiccare.backend.report;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReportProgressService {
    private final ReportRepository reports;
    private final ReportProgressUpdateRepository updates;
    private final PrivateImageStorage storage;
    public ReportProgressService(ReportRepository reports, ReportProgressUpdateRepository updates, PrivateImageStorage storage) { this.reports = reports; this.updates = updates; this.storage = storage; }

    @Transactional
    public AdminReportDtos.ProgressUpdateResponse create(Long reportId, ReportStatus status, String note, MultipartFile image) {
        if (status == null) throw new IllegalArgumentException("A valid status is required.");
        if (note != null && note.length() > 1000) throw new IllegalArgumentException("Update note must be 1000 characters or fewer.");
        PublicCareReport report = reports.findById(reportId).orElseThrow(ReportNotFoundException::new);
        String key = null, type = null; Long size = null;
        if (image != null && !image.isEmpty()) {
            if (image.getSize() > ReportImageService.MAX_IMAGE_BYTES) throw new IllegalArgumentException("Image must be 5 MB or smaller.");
            byte[] bytes;
            try { bytes = image.getBytes(); } catch (IOException e) { throw new IllegalArgumentException("Unable to read uploaded image."); }
            if (bytes.length == 0 || bytes.length > ReportImageService.MAX_IMAGE_BYTES) throw new IllegalArgumentException("Image must be between 1 byte and 5 MB.");
            type = validate(bytes);
            key = storage.store(bytes, type.equals("image/jpeg") ? "jpg" : "png"); size = (long) bytes.length;
        }
        try {
            if (report.getStatus() != status) report.revokePublicApproval();
            report.setStatus(status);
            report.setAdminUpdateNote(note == null ? null : note.trim());
            ReportProgressUpdate update = updates.saveAndFlush(new ReportProgressUpdate(report, status, note == null ? null : note.trim(), key, type, size));
            return adminResponse(update);
        } catch (RuntimeException e) { if (key != null) storage.delete(key); throw e; }
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.ProgressUpdateResponse> citizenList(Long ownerId, Long reportId) {
        reports.findOne(ReportService.ownerAndId(ownerId, reportId)).orElseThrow(ReportNotFoundException::new);
        return updates.findByReportIdOrderByCreatedAtAscIdAsc(reportId).stream().map(ReportProgressService::response).toList();
    }
    @Transactional(readOnly = true)
    public List<AdminReportDtos.ProgressUpdateResponse> adminList(Long reportId) {
        if (!reports.existsById(reportId)) throw new ReportNotFoundException();
        return updates.findByReportIdOrderByCreatedAtAscIdAsc(reportId).stream().map(ReportProgressService::adminResponse).toList();
    }
    @Transactional(readOnly = true)
    public ReportImageService.ImageContent citizenImage(Long ownerId, Long reportId, Long updateId) {
        reports.findOne(ReportService.ownerAndId(ownerId, reportId)).orElseThrow(ReportNotFoundException::new);
        return image(reportId, updateId);
    }
    @Transactional(readOnly = true)
    public ReportImageService.ImageContent adminImage(Long reportId, Long updateId) {
        if (!reports.existsById(reportId)) throw new ReportNotFoundException();
        return image(reportId, updateId);
    }
    @Transactional
    public boolean setPublicApproval(Long reportId, Long updateId, boolean approved, Long adminId) {
        if (!reports.existsById(reportId)) throw new ReportNotFoundException();
        ReportProgressUpdate update = updates.findByIdAndReportId(updateId, reportId).orElseThrow(ReportNotFoundException::new);
        if (approved) update.approveForPublic(adminId);
        else update.revokePublicApproval();
        updates.save(update);
        return update.isPublicApproved();
    }
    @Transactional
    public boolean setNotePublicApproval(Long reportId, Long updateId, boolean approved, Long adminId) {
        if (!reports.existsById(reportId)) throw new ReportNotFoundException();
        ReportProgressUpdate update = updates.findByIdAndReportId(updateId, reportId).orElseThrow(ReportNotFoundException::new);
        if (approved) update.approveNoteForPublic(adminId);
        else update.revokeNotePublicApproval();
        updates.save(update);
        return update.isNotePublicApproved();
    }
    private ReportImageService.ImageContent image(Long reportId, Long updateId) {
        ReportProgressUpdate update = updates.findByIdAndReportId(updateId, reportId).filter(item -> item.getStorageKey() != null).orElseThrow(ReportNotFoundException::new);
        return new ReportImageService.ImageContent(storage.read(update.getStorageKey()), update.getContentType());
    }
    private static ReportDtos.ProgressUpdateResponse response(ReportProgressUpdate u) { return new ReportDtos.ProgressUpdateResponse(u.getId(), u.getStatus(), u.getNote(), u.getCreatedAt(), u.getStorageKey() != null); }
    private static AdminReportDtos.ProgressUpdateResponse adminResponse(ReportProgressUpdate u) {
        return new AdminReportDtos.ProgressUpdateResponse(u.getId(), u.getStatus(), u.getNote(), u.getCreatedAt(),
                u.getStorageKey() != null, u.isPublicApproved(), u.isNotePublicApproved());
    }
    private static String validate(byte[] bytes) {
        String type;
        if (bytes.length >= 3 && (bytes[0]&255)==255 && (bytes[1]&255)==216 && (bytes[2]&255)==255) type="image/jpeg";
        else if (bytes.length >= 8 && (bytes[0]&255)==137 && bytes[1]==80 && bytes[2]==78 && bytes[3]==71 && bytes[4]==13 && bytes[5]==10 && bytes[6]==26 && bytes[7]==10) type="image/png";
        else throw new IllegalArgumentException("Only valid JPG or PNG images are supported.");
        try {
            var image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1 || (long)image.getWidth()*image.getHeight() > 10_000_000L) throw new IllegalArgumentException("Uploaded file is not a valid JPG or PNG image.");
        } catch (IOException e) { throw new IllegalArgumentException("Uploaded file is not a valid JPG or PNG image."); }
        return type;
    }
}
