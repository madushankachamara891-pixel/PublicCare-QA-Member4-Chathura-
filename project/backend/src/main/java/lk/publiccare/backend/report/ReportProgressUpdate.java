package lk.publiccare.backend.report;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "report_progress_updates")
public class ReportProgressUpdate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "report_id", nullable = false, updatable = false) private PublicCareReport report;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private ReportStatus status;
    @Column(length = 1000) private String note;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "storage_key", unique = true, length = 80) private String storageKey;
    @Column(name = "content_type", length = 32) private String contentType;
    @Column(name = "size_bytes") private Long sizeBytes;
    @Column(name = "public_approved", nullable = false) private boolean publicApproved;
    @Column(name = "last_public_approved_by") private Long lastPublicApprovedBy;
    @Column(name = "last_public_approved_at") private Instant lastPublicApprovedAt;
    @Column(name = "note_public_approved", nullable = false) private boolean notePublicApproved;
    @Column(name = "last_note_public_approved_by") private Long lastNotePublicApprovedBy;
    @Column(name = "last_note_public_approved_at") private Instant lastNotePublicApprovedAt;
    protected ReportProgressUpdate() { }
    public ReportProgressUpdate(PublicCareReport report, ReportStatus status, String note, String storageKey, String contentType, Long sizeBytes) {
        this.report = report; this.status = status; this.note = note; this.storageKey = storageKey; this.contentType = contentType; this.sizeBytes = sizeBytes; this.createdAt = Instant.now();
    }
    public Long getId() { return id; } public ReportStatus getStatus() { return status; } public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; } public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; } public Long getSizeBytes() { return sizeBytes; }
    public boolean isPublicApproved() { return publicApproved; }
    public boolean isNotePublicApproved() { return notePublicApproved; }
    public void approveForPublic(Long adminId) { publicApproved = true; lastPublicApprovedBy = adminId; lastPublicApprovedAt = Instant.now(); }
    public void revokePublicApproval() { publicApproved = false; }
    public void approveNoteForPublic(Long adminId) {
        if (note == null || note.isBlank()) throw new IllegalArgumentException("A non-empty note is required for public approval.");
        notePublicApproved = true; lastNotePublicApprovedBy = adminId; lastNotePublicApprovedAt = Instant.now();
    }
    public void revokeNotePublicApproval() { notePublicApproved = false; }
}
