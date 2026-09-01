package ng.edu.unilag.tradeup.service;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import ng.edu.unilag.tradeup.domain.Category;
import ng.edu.unilag.tradeup.domain.Condition;
import ng.edu.unilag.tradeup.domain.TradeIntent;
import ng.edu.unilag.tradeup.web.dto.ReferenceData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serves the enum vocabularies to the frontend.
 *
 * <p>Filters, dropdowns and badges are all built from this one response, so
 * adding a category is a change to {@link Category} and nothing else.
 */
@Service
public class ReferenceService {

    private final ListingService listingService;

    public ReferenceService(ListingService listingService) {
        this.listingService = listingService;
    }

    @Transactional(readOnly = true)
    public ReferenceData load() {
        Map<Category, Long> available = availableCounts();

        List<ReferenceData.CategoryOption> categories = Arrays.stream(Category.values())
                .map(category -> new ReferenceData.CategoryOption(
                        category.name(),
                        category.label(),
                        category.slug(),
                        available.getOrDefault(category, 0L)))
                .toList();

        List<ReferenceData.Option> conditions = Arrays.stream(Condition.values())
                .map(condition ->
                        new ReferenceData.Option(condition.name(), condition.label(), condition.description()))
                .toList();

        List<ReferenceData.Option> intents = Arrays.stream(TradeIntent.values())
                .map(intent -> new ReferenceData.Option(intent.name(), intent.label(), null))
                .toList();

        return new ReferenceData(categories, conditions, intents);
    }

    /** One grouped query, unpacked into a map keyed by category. */
    private Map<Category, Long> availableCounts() {
        Map<Category, Long> counts = new EnumMap<>(Category.class);
        for (Object[] row : listingService.availableCountsByCategory()) {
            counts.put((Category) row[0], (Long) row[1]);
        }
        return counts;
    }
}
