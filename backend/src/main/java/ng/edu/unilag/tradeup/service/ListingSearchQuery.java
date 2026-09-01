package ng.edu.unilag.tradeup.service;

import java.util.List;
import ng.edu.unilag.tradeup.domain.Category;
import ng.edu.unilag.tradeup.domain.Condition;
import ng.edu.unilag.tradeup.domain.TradeIntent;
import org.springframework.data.domain.Sort;

/**
 * Everything the browse screen can ask for, in one object.
 *
 * <p>Any field may be null, which means the student did not use that filter.
 *
 * @param keyword free text matched against title, description and swap notes
 * @param category restrict to a single category
 * @param conditions accept any of these conditions
 * @param intent sale, swap, or either
 * @param minPriceKobo lower price bound
 * @param maxPriceKobo upper price bound
 * @param sort how to order the results
 * @param page zero-based page number
 * @param size results per page
 */
public record ListingSearchQuery(
        String keyword,
        Category category,
        List<Condition> conditions,
        TradeIntent intent,
        Long minPriceKobo,
        Long maxPriceKobo,
        SortOption sort,
        int page,
        int size) {

    /** The orderings offered in the browse dropdown. */
    public enum SortOption {
        NEWEST("createdAt", Sort.Direction.DESC),
        PRICE_LOW("priceKobo", Sort.Direction.ASC),
        PRICE_HIGH("priceKobo", Sort.Direction.DESC),
        POPULAR("viewCount", Sort.Direction.DESC),
        BEST_CONDITION("itemCondition", Sort.Direction.ASC);

        private final String property;
        private final Sort.Direction direction;

        SortOption(String property, Sort.Direction direction) {
            this.property = property;
            this.direction = direction;
        }

        /**
         * Sorts by the chosen property, then by id so that items sharing a value
         * keep a stable order across pages instead of shuffling between requests.
         */
        public Sort toSort() {
            return Sort.by(direction, property).and(Sort.by(Sort.Direction.DESC, "id"));
        }

        public static SortOption parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return NEWEST;
            }
            try {
                return valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return NEWEST;
            }
        }
    }
}
