package ng.edu.unilag.tradeup.repository;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import ng.edu.unilag.tradeup.domain.Category;
import ng.edu.unilag.tradeup.domain.Condition;
import ng.edu.unilag.tradeup.domain.Listing;
import ng.edu.unilag.tradeup.domain.ListingStatus;
import ng.edu.unilag.tradeup.domain.TradeIntent;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable pieces of the browse query.
 *
 * <p>Each method returns one predicate, or null when that filter was not
 * supplied. {@link #none()} is the identity element, so the service can fold an
 * arbitrary set of user-chosen filters together with {@code and} and never has
 * to special-case the empty search.
 */
public final class ListingSpecifications {

    private ListingSpecifications() {
        // Utility class.
    }

    /** Matches everything. The starting point for folding filters together. */
    public static Specification<Listing> none() {
        return (root, query, cb) -> cb.conjunction();
    }

    /** Free-text search across the title, description and swap wishlist. */
    public static Specification<Listing> matchesKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("swapWanted"), "")), pattern));
    }

    public static Specification<Listing> inCategory(Category category) {
        return category == null ? null : (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Listing> hasCondition(Collection<Condition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("itemCondition").in(conditions);
    }

    public static Specification<Listing> hasIntent(TradeIntent intent) {
        if (intent == null) {
            return null;
        }
        // Asking for SELL should also surface items marked as sale-or-swap.
        return (root, query, cb) -> switch (intent) {
            case SELL -> root.get("intent").in(List.of(TradeIntent.SELL, TradeIntent.BOTH));
            case SWAP -> root.get("intent").in(List.of(TradeIntent.SWAP, TradeIntent.BOTH));
            case BOTH -> cb.equal(root.get("intent"), TradeIntent.BOTH);
        };
    }

    /** Lower bound in kobo. Swap-only listings have no price and are excluded. */
    public static Specification<Listing> priceAtLeast(Long minKobo) {
        return minKobo == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("priceKobo"), minKobo);
    }

    /**
     * Upper bound in kobo. Swap-only listings are kept, because "under 5,000"
     * should still show the items that cost nothing at all.
     */
    public static Specification<Listing> priceAtMost(Long maxKobo) {
        return maxKobo == null
                ? null
                : (root, query, cb) -> cb.or(
                        cb.isNull(root.get("priceKobo")), cb.lessThanOrEqualTo(root.get("priceKobo"), maxKobo));
    }

    public static Specification<Listing> hasStatus(ListingStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Only the statuses a student browsing the site is allowed to see. */
    public static Specification<Listing> publiclyVisible() {
        return (root, query, cb) -> root.get("status").in(List.of(ListingStatus.ACTIVE, ListingStatus.RESERVED));
    }

    public static Specification<Listing> ownedBy(Long ownerId) {
        return ownerId == null ? null : (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    /**
     * Folds any number of specifications together with AND, skipping the nulls
     * that stand for a filter the student did not use.
     */
    @SafeVarargs
    public static Specification<Listing> allOf(Specification<Listing>... specs) {
        List<Specification<Listing>> supplied = new ArrayList<>();
        for (Specification<Listing> spec : specs) {
            if (spec != null) {
                supplied.add(spec);
            }
        }
        Specification<Listing> combined = none();
        for (Specification<Listing> spec : supplied) {
            combined = combined.and(spec);
        }
        return combined;
    }

    /** Kept for readability at call sites that build a predicate list by hand. */
    static Predicate andAll(jakarta.persistence.criteria.CriteriaBuilder cb, List<Predicate> predicates) {
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
