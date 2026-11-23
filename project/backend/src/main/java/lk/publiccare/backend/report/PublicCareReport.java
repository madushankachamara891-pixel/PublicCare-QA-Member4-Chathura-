package lk.publiccare.backend.report;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lk.publiccare.backend.user.PublicCareUser;

@Entity
@Table(name = "reports")
public class PublicCareReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", nullable = false, unique = true, insertable = false, updatable = false, length = 20)
    private String reference;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 240)
    private String location;

    @Column(name = "admin_update_note", length = 1000)
    private String adminUpdateNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private PublicCareUser owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "public_approved", nullable = false)
    private boolean publicApproved;

    @Column(name = "last_public_approved_by")
    private Long lastPublicApprovedBy;

    @Column(name = "last_public_approved_at")
    private Instant lastPublicApprovedAt;

    protected PublicCareReport() { }

    public PublicCareReport(String title, String description, String category, String location, PublicCareUser owner) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.owner = owner;
    }

    @PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = ReportStatus.PENDING;
    }

    @PreUpdate
    void beforeUpdate() { updatedAt = Instant.now(); }

    public void updateDetails(String title, String description, String category, String location) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
    }

    public void setStatus(ReportStatus status) { this.status = status; }
    public void approveForPublic(Long adminId) {
        publicApproved = true;
        lastPublicApprovedBy = adminId;
        lastPublicApprovedAt = Instant.now();
    }
    public void revokePublicApproval() { publicApproved = false; }
    public Long getId() { return id; }
    public String getReference() { return reference; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public ReportStatus getStatus() { return status; }
    public String getAdminUpdateNote() { return adminUpdateNote; }
    public void setAdminUpdateNote(String note) { this.adminUpdateNote = note; }
    public PublicCareUser getOwner() { return owner; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public boolean isPublicApproved() { return publicApproved; }
}
