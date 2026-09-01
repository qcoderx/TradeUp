package ng.edu.unilag.tradeup.web.dto;

import java.util.List;

/**
 * The SDG 12 figures for the whole marketplace.
 *
 * <p>Every number here is computed from real completed trades rather than being
 * a decorative counter, which is the point: the impact claim in the proposal has
 * to be something the application can actually evidence.
 */
public record ImpactSnapshot(
        long itemsRehomed,
        long itemsAvailableNow,
        long studentsRegistered,
        double co2SavedKg,
        double wasteDivertedKg,
        long moneyKeptInPocketsKobo,
        List<CategoryImpact> byCategory) {

    /** Per-category breakdown, used for the impact chart. */
    public record CategoryImpact(String name, String label, String slug, long itemsRehomed, double co2SavedKg) {}
}
