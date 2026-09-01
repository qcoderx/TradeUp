package ng.edu.unilag.tradeup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import ng.edu.unilag.tradeup.web.error.InvalidTransitionException;

/** A student flagging a listing for a moderator to look at. */
@Entity
@Table(name = "reports")
public class Report extends BaseEntity {

    /** Why the listing was flagged. */
    public enum Reason {
        SPAM("Spam or repeated posting"),
        PROHIBITED("Prohibited item"),
        MISLEADING("Misleading description or photos"),
        OFFENSIVE("Offensive content"),
        SCAM("Suspected scam"),
        OTHER("Something else");

        private final String label;

        Reason(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Where the moderator got to with it. */
    public enum Status {
        OPEN,
        UPHELD,
        DISMISSED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Reason reason;

    @Column(length = 700)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status = Status.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    private Instant reviewedAt;

    @Column(length = 500)
    private String moderatorNote;

    protected Report() {
        // Required by JPA.
    }

    public Report(Listing listing, User reporter, Reason reason, String details) {
        this.listing = Objects.requireNonNull(listing, "listing");
        this.reporter = Objects.requireNonNull(reporter, "reporter");
        this.reason = reason;
        this.details = details;
    }

    /** The report was justified: the listing comes down. */
    public void uphold(User moderator, String note) {
        close(Status.UPHELD, moderator, note);
        listing.flag();
    }

    /** Nothing wrong here: the listing goes back up if it was pulled. */
    public void dismiss(User moderator, String note) {
        close(Status.DISMISSED, moderator, note);
        if (listing.getStatus() == ListingStatus.FLAGGED) {
            listing.reinstate();
        }
    }

    private void close(Status outcome, User moderator, String note) {
        if (status != Status.OPEN) {
            throw new InvalidTransitionException("This report has already been reviewed.");
        }
        this.status = outcome;
        this.reviewedBy = moderator;
        this.reviewedAt = Instant.now();
        this.moderatorNote = note;
    }

    public Listing getListing() {
        return listing;
    }

    public User getReporter() {
        return reporter;
    }

    public Reason getReason() {
        return reason;
    }

    public String getDetails() {
        return details;
    }

    public Status getStatus() {
        return status;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getModeratorNote() {
        return moderatorNote;
    }
}
