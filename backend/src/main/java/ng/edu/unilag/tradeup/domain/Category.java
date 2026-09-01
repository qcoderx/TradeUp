package ng.edu.unilag.tradeup.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * The kinds of things that actually change hands on a Nigerian campus.
 *
 * <p>Each constant carries the average mass of an item in that category and the
 * kilograms of CO2e avoided when one is reused instead of bought new. Those two
 * numbers are what let {@code ImpactService} report real SDG 12 figures rather
 * than a decorative counter.
 */
public enum Category {

    TEXTBOOKS("Textbooks", "textbooks", 1.2, 7.5),
    ELECTRONICS("Electronics", "electronics", 1.8, 42.0),
    LAB_EQUIPMENT("Lab equipment", "lab-equipment", 0.9, 12.0),
    FURNITURE("Furniture", "furniture", 14.0, 38.0),
    STATIONERY("Stationery", "stationery", 0.3, 1.4),
    HOSTEL_ESSENTIALS("Hostel essentials", "hostel-essentials", 3.5, 15.0),
    CLOTHING("Clothing", "clothing", 0.6, 9.0),
    SPORTS("Sports & fitness", "sports", 2.4, 11.0),
    OTHER("Other", "other", 1.0, 5.0);

    private final String label;
    private final String slug;
    private final double averageWeightKg;
    private final double co2SavedKgPerReuse;

    Category(String label, String slug, double averageWeightKg, double co2SavedKgPerReuse) {
        this.label = label;
        this.slug = slug;
        this.averageWeightKg = averageWeightKg;
        this.co2SavedKgPerReuse = co2SavedKgPerReuse;
    }

    public String label() {
        return label;
    }

    public String slug() {
        return slug;
    }

    public double averageWeightKg() {
        return averageWeightKg;
    }

    public double co2SavedKgPerReuse() {
        return co2SavedKgPerReuse;
    }

    /** Resolves a category from either its enum name or its URL slug, case-insensitively. */
    public static Optional<Category> fromSlug(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String needle = value.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(c -> c.slug.equals(needle) || c.name().toLowerCase().equals(needle))
                .findFirst();
    }
}
