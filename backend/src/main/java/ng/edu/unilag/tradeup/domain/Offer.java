package ng.edu.unilag.tradeup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import ng.edu.unilag.tradeup.web.error.InvalidTransitionException;

/**
 * A concrete proposal on a listing: either an amount of cash, or another
 * listing put up in trade.
 */
@Entity
@Table(name = "offers")
public class Offer extends BaseEntity {

    /** What kind of proposal this is. */
    public enum Kind {
        CASH,
        SWAP
    }

    /** Where the proposal has got to. */
    public enum Status {
        PENDING,
        ACCEPTED,
        DECLINED,
        WITHDRAWN
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offered_by_id", nullable = false)
    private User offeredBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Kind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status = Status.PENDING;

    /** Set for a CASH offer. */
    private Long amountKobo;

    /** Set for a SWAP offer: the item being put up in trade. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_listing_id")
    private Listing offeredListing;

    @Column(length = 500)
    private String note;

    protected Offer() {
        // Required by JPA.
    }

    private Offer(Listing listing, User offeredBy, Kind kind, String note) {
        this.listing = Objects.requireNonNull(listing, "listing");
        this.offeredBy = Objects.requireNonNull(offeredBy, "offeredBy");
        this.kind = kind;
        this.note = note;
    }

    public static Offer cash(Listing listing, User offeredBy, long amountKobo, String note) {
        if (amountKobo <= 0) {
            throw new IllegalArgumentException("A cash offer must be greater than zero.");
        }
        Offer offer = new Offer(listing, offeredBy, Kind.CASH, note);
        offer.amountKobo = amountKobo;
        return offer;
    }

    public static Offer swap(Listing listing, User offeredBy, Listing offeredListing, String note) {
        Objects.requireNonNull(offeredListing, "offeredListing");
        if (!offeredListing.isOwnedBy(offeredBy)) {
            throw new IllegalArgumentException("You can only offer an item you own.");
        }
        Offer offer = new Offer(listing, offeredBy, Kind.SWAP, note);
        offer.offeredListing = offeredListing;
        return offer;
    }

    public void accept() {
        requirePending("accept");
        this.status = Status.ACCEPTED;
    }

    public void decline() {
        requirePending("decline");
        this.status = Status.DECLINED;
    }

    public void withdraw() {
        requirePending("withdraw");
        this.status = Status.WITHDRAWN;
    }

    private void requirePending(String action) {
        if (status != Status.PENDING) {
            throw new InvalidTransitionException(
                    "You cannot %s an offer that was already %s.".formatted(action, status.name().toLowerCase()));
        }
    }

    public Listing getListing() {
        return listing;
    }

    public User getOfferedBy() {
        return offeredBy;
    }

    public Kind getKind() {
        return kind;
    }

    public Status getStatus() {
        return status;
    }

    public Long getAmountKobo() {
        return amountKobo;
    }

    public Listing getOfferedListing() {
        return offeredListing;
    }

    public String getNote() {
        return note;
    }
}
