package ng.edu.unilag.tradeup.service;

import java.util.List;
import ng.edu.unilag.tradeup.domain.Listing;
import ng.edu.unilag.tradeup.domain.Report;
import ng.edu.unilag.tradeup.domain.User;
import ng.edu.unilag.tradeup.repository.ReportRepository;
import ng.edu.unilag.tradeup.web.dto.ReportRequest;
import ng.edu.unilag.tradeup.web.dto.ReportView;
import ng.edu.unilag.tradeup.web.error.NotFoundException;
import ng.edu.unilag.tradeup.web.error.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flagging listings and the moderator queue that reviews them.
 *
 * <p>Reporting is open to any signed-in student; acting on a report is
 * restricted to moderators by the URL rules in {@code SecurityConfig}.
 */
@Service
public class ModerationService {

    private final ReportRepository reportRepository;
    private final ListingService listingService;

    public ModerationService(ReportRepository reportRepository, ListingService listingService) {
        this.reportRepository = reportRepository;
        this.listingService = listingService;
    }

    /** A student flags a listing. One open report per student per listing. */
    @Transactional
    public ReportView report(Long listingId, User reporter, ReportRequest request) {
        Listing listing = listingService.requireListing(listingId);

        if (listing.isOwnedBy(reporter)) {
            throw new ValidationException("You cannot report your own listing.");
        }
        if (reportRepository.existsByListingIdAndReporterIdAndStatus(listingId, reporter.getId(), Report.Status.OPEN)) {
            throw new ValidationException("You have already reported this listing. A moderator is looking at it.");
        }

        Report report = new Report(listing, reporter, request.reason(), trimToNull(request.details()));
        return ReportView.from(reportRepository.save(report));
    }

    /** The moderator queue. */
    @Transactional(readOnly = true)
    public List<ReportView> queue(Report.Status status) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(ReportView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long openReportCount() {
        return reportRepository.countByStatus(Report.Status.OPEN);
    }

    /** The report was justified. The listing is pulled from public view. */
    @Transactional
    public ReportView uphold(Long reportId, User moderator, String note) {
        Report report = requireReport(reportId);
        report.uphold(moderator, trimToNull(note));
        return ReportView.from(report);
    }

    /** Nothing wrong with the listing. It goes back up if it had been pulled. */
    @Transactional
    public ReportView dismiss(Long reportId, User moderator, String note) {
        Report report = requireReport(reportId);
        report.dismiss(moderator, trimToNull(note));
        return ReportView.from(report);
    }

    private Report requireReport(Long id) {
        return reportRepository.findById(id).orElseThrow(() -> NotFoundException.of("Report", id));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
