package ng.edu.unilag.tradeup.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

/** A listing a student has kept an eye on. */
@Entity
@Table(
        name = "saved_listings",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_saved_user_listing",
                        columnNames = {"user_id", "listing_id"}))
public class SavedListing extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    protected SavedListing() {
        // Required by JPA.
    }

    public SavedListing(User user, Listing listing) {
        this.user = Objects.requireNonNull(user, "user");
        this.listing = Objects.requireNonNull(listing, "listing");
    }

    public User getUser() {
        return user;
    }

    public Listing getListing() {
        return listing;
    }
}
