package ng.edu.unilag.tradeup.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ng.edu.unilag.tradeup.web.error.InvalidTransitionException;

/**
 * An item a student is offering to the campus, for cash, for a swap, or either.
 *
 * <p>The status transitions live here rather than in the service layer, so a
 * listing can never be moved into a state that does not make sense no matter
 * which caller is asking.
 */
@Entity
@Table(
        name = "listings",
        indexes = {
            @Index(name = "idx_listing_status", columnList = "status"),
            @Index(name = "idx_listing_category", columnList = "category"),
            @Index(name = "idx_listing_reference", columnList = "reference", unique = true)
        })
public class Listing extends BaseEntity {

    /** Short human-readable code printed on the trade ticket, e.g. {@code TU-7QK42}. */
    @Column(nullable = false, unique = true, length = 12)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Condition itemCondition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradeIntent intent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListingStatus status = ListingStatus.ACTIVE;

    /** Asking price in kobo. Null when the listing is swap-only. */
    private Long priceKobo;

    /** Free text describing what the lister would accept in trade. */
    @Column(length = 300)
    private String swapWanted;

    @Column(length = 80)
    private String pickupLocation;

    /**
     * Batched deliberately. Without this Hibernate fetches the photos for each
     * listing in its own statement, so a page of 24 costs 24 extra round trips
     * to the database. The batch turns them into a single IN query.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listing_images", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "position")
    @Column(name = "url", length = 400)
    @BatchSize(size = 64)
    private List<String> imageUrls = new ArrayList<>();

    /**
     * How many students have owned this item, counting the current one. A value
     * of 3 renders on the ticket as "3rd owner" — the provenance that makes
     * reuse visible rather than invisible.
     */
    @Column(nullable = false)
    private int ownerGeneration = 1;

    @Column(nullable = false)
    private int viewCount = 0;

    /** Guards against two moderators editing the same listing at once. */
    @Version
    private Long version;

    protected Listing() {
        // Required by JPA.
    }

    public Listing(
            String reference,
            User owner,
            String title,
            String description,
            Category category,
            Condition itemCondition,
            TradeIntent intent) {
        this.reference = Objects.requireNonNull(reference, "reference");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.title = title;
        this.description = description;
        this.category = category;
        this.itemCondition = itemCondition;
        this.intent = intent;
    }

    // ---------------------------------------------------------------------
    // Behaviour
    // ---------------------------------------------------------------------

    /**
     * True when {@code candidate} is the student who created this listing.
     *
     * <p>The null-id guard matters: two unsaved entities both have a null id, and
     * comparing those as equal would make an unsaved listing look like it belongs
     * to whoever asked. This method gates permissions, so it fails closed.
     */
    public boolean isOwnedBy(User candidate) {
        if (candidate == null || owner == null || owner.getId() == null || candidate.getId() == null) {
            return false;
        }
        return owner.getId().equals(candidate.getId());
    }

    /** Someone is interested; hold the item without completing the trade yet. */
    public void reserve() {
        requireStatus(ListingStatus.ACTIVE, "reserve");
        this.status = ListingStatus.RESERVED;
    }

    /** The hold fell through and the item is available again. */
    public void release() {
        requireStatus(ListingStatus.RESERVED, "release");
        this.status = ListingStatus.ACTIVE;
    }

    /**
     * The handover happened. The owner generation increases so that if this item
     * is ever listed again, the next student can see how far it has travelled.
     */
    public void complete() {
        if (status != ListingStatus.ACTIVE && status != ListingStatus.RESERVED) {
            throw new InvalidTransitionException(
                    "A listing that is %s cannot be completed.".formatted(status.label().toLowerCase()));
        }
        this.status = ListingStatus.COMPLETED;
        this.ownerGeneration++;
        this.owner.recordCompletedTrade();
    }

    /** Taken down by the owner. */
    public void remove() {
        if (status.isTerminal()) {
            throw new InvalidTransitionException("This listing has already been closed.");
        }
        this.status = ListingStatus.REMOVED;
    }

    /** Hidden pending moderator review after a report was upheld. */
    public void flag() {
        this.status = ListingStatus.FLAGGED;
    }

    /** A moderator dismissed the report; put it back on the board. */
    public void reinstate() {
        requireStatus(ListingStatus.FLAGGED, "reinstate");
        this.status = ListingStatus.ACTIVE;
    }

    public void recordView() {
        this.viewCount++;
    }

    /**
     * The CO2e in kilograms this trade keeps out of the atmosphere, derived from
     * the category baseline and discounted for items that are nearly worn out.
     */
    public double estimatedCo2SavedKg() {
        double conditionFactor =
                switch (itemCondition) {
                    case NEW, LIKE_NEW -> 1.0;
                    case GOOD -> 0.9;
                    case FAIR -> 0.75;
                    case WELL_USED -> 0.6;
                };
        return category.co2SavedKgPerReuse() * conditionFactor;
    }

    private void requireStatus(ListingStatus expected, String action) {
        if (status != expected) {
            throw new InvalidTransitionException(
                    "Cannot %s a listing that is %s.".formatted(action, status.label().toLowerCase()));
        }
    }

    // ---------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------

    public String getReference() {
        return reference;
    }

    public User getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Condition getItemCondition() {
        return itemCondition;
    }

    public void setItemCondition(Condition itemCondition) {
        this.itemCondition = itemCondition;
    }

    public TradeIntent getIntent() {
        return intent;
    }

    public void setIntent(TradeIntent intent) {
        this.intent = intent;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }

    public Long getPriceKobo() {
        return priceKobo;
    }

    public void setPriceKobo(Long priceKobo) {
        this.priceKobo = priceKobo;
    }

    public String getSwapWanted() {
        return swapWanted;
    }

    public void setSwapWanted(String swapWanted) {
        this.swapWanted = swapWanted;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls == null ? new ArrayList<>() : new ArrayList<>(imageUrls);
    }

    public int getOwnerGeneration() {
        return ownerGeneration;
    }

    public void setOwnerGeneration(int ownerGeneration) {
        this.ownerGeneration = ownerGeneration;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }
}
