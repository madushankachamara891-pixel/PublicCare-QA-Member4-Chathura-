package lk.publiccare.backend.report;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ReportDeletionService {
    private static final Logger logger = LoggerFactory.getLogger(ReportDeletionService.class);

    private final ReportRepository reports;
    private final ReportImageRepository images;
    private final ReportProgressUpdateRepository progressUpdates;
    private final PrivateImageStorage storage;

    public ReportDeletionService(ReportRepository reports, ReportImageRepository images,
            ReportProgressUpdateRepository progressUpdates, PrivateImageStorage storage) {
        this.reports = reports;
        this.images = images;
        this.progressUpdates = progressUpdates;
        this.storage = storage;
    }

    @Transactional
    public void deleteAdmin(Long reportId) {
        PublicCareReport report = reports.findById(reportId).orElseThrow(ReportNotFoundException::new);
        List<String> storageKeys = new ArrayList<>();
        List<ReportImage> reportImages = images.findByReportId(reportId).stream().toList();
        reportImages.stream().map(ReportImage::getStorageKey).forEach(storageKeys::add);
        List<ReportProgressUpdate> updates = progressUpdates.findByReportIdOrderByCreatedAtAscIdAsc(reportId);
        updates.stream()
                .map(ReportProgressUpdate::getStorageKey)
                .filter(key -> key != null)
                .forEach(storageKeys::add);

        images.deleteAll(reportImages);
        progressUpdates.deleteAll(updates);
        images.flush();
        progressUpdates.flush();

        reports.delete(report);
        reports.flush();

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Report deletion requires an active transaction.");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String storageKey : storageKeys) {
                    try {
                        storage.delete(storageKey);
                    } catch (RuntimeException exception) {
                        logger.error("Unable to remove private image after deleting report {} (storage key {}).",
                                reportId, storageKey, exception);
                    }
                }
            }
        });
    }
}
