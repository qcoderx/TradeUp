package ng.edu.unilag.tradeup.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import ng.edu.unilag.tradeup.domain.Category;
import ng.edu.unilag.tradeup.domain.Listing;
import ng.edu.unilag.tradeup.domain.ListingStatus;
import ng.edu.unilag.tradeup.domain.User;
import ng.edu.unilag.tradeup.repository.ListingRepository;
import ng.edu.unilag.tradeup.repository.UserRepository;
import ng.edu.unilag.tradeup.web.dto.ImpactSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns completed trades into the SDG 12 figures the proposal promises.
 *
 * <p>Nothing here is a hard-coded marketing number. Each completed trade
 * contributes the CO2e its own category and condition imply, so the headline on
 * the landing page is something the database can actually account for.
 */
@Service
public class ImpactService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ImpactService(ListingRepository listingRepository, UserRepository userRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    /** Marketplace-wide totals, with a per-category breakdown for the chart. */
    @Transactional(readOnly = true)
    public ImpactSnapshot snapshot() {
        List<Listing> completed = listingRepository.findCompletedTrades();

        // EnumMap keeps the categories in declaration order for a stable chart.
        Map<Category, long[]> counts = new EnumMap<>(Category.class);
        Map<Category, double[]> co2 = new EnumMap<>(Category.class);

        double totalCo2 = 0;
        double totalWaste = 0;

        for (Listing listing : completed) {
            Category category = listing.getCategory();
            double saved = listing.estimatedCo2SavedKg();

            counts.computeIfAbsent(category, key -> new long[1])[0]++;
            co2.computeIfAbsent(category, key -> new double[1])[0] += saved;

            totalCo2 += saved;
            totalWaste += category.averageWeightKg();
        }

        List<ImpactSnapshot.CategoryImpact> byCategory = counts.entrySet().stream()
                .map(entry -> new ImpactSnapshot.CategoryImpact(
                        entry.getKey().name(),
                        entry.getKey().label(),
                        entry.getKey().slug(),
                        entry.getValue()[0],
                        round(co2.get(entry.getKey())[0])))
                .toList();

        return new ImpactSnapshot(
                completed.size(),
                listingRepository.countByStatus(ListingStatus.ACTIVE),
                userRepository.count(),
                round(totalCo2),
                round(totalWaste),
                listingRepository.sumPriceKoboByStatus(ListingStatus.COMPLETED),
                byCategory);
    }

    /** One students share of the total, shown on their dashboard. */
    @Transactional(readOnly = true)
    public double personalCo2SavedKg(User user) {
        return round(listingRepository.findByOwnerIdAndStatus(user.getId(), ListingStatus.COMPLETED).stream()
                .mapToDouble(Listing::estimatedCo2SavedKg)
                .sum());
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
