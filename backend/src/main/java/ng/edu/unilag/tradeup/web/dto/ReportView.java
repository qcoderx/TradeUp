package ng.edu.unilag.tradeup.web.dto;

import java.time.Instant;
import ng.edu.unilag.tradeup.domain.Report;

/** A report as it appears in the moderation queue. */
public record ReportView(
        Long id,
        String reason,
        String reasonLabel,
        String details,
        String status,
        UserSummary reporter,
        Long listingId,
        String listingTitle,
        String listingReference,
        String listingImageUrl,
        String listingStatus,
        UserSummary listingOwner,
        String moderatorNote,
        Instant reviewedAt,
        Instant createdAt) {

    public static ReportView from(Report report) {
        var listing = report.getListing();
        return new ReportView(
                report.getId(),
                report.getReason().name(),
                report.getReason().label(),
                report.getDetails(),
                report.getStatus().name(),
                UserSummary.from(report.getReporter()),
                listing.getId(),
                listing.getTitle(),
                listing.getReference(),
                listing.getImageUrls().isEmpty() ? null : listing.getImageUrls().get(0),
                listing.getStatus().name(),
                UserSummary.from(listing.getOwner()),
                report.getModeratorNote(),
                report.getReviewedAt(),
                report.getCreatedAt());
    }
}
