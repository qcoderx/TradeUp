package ng.edu.unilag.tradeup.web.dto;

import java.time.Instant;
import ng.edu.unilag.tradeup.domain.Listing;

/**
 * A listing as it appears on the trade ticket in a grid.
 *
 * <p>Enums are flattened into a name plus a display label so the frontend never
 * has to keep its own copy of the wording, and {@code ownerGeneration} is what
 * the ticket prints as the provenance stamp.
 */
public record ListingCard(
        Long id,
        String reference,
        String title,
        String categoryName,
        String categoryLabel,
        String categorySlug,
        String conditionName,
        String conditionLabel,
        String intentName,
        String intentLabel,
        String statusName,
        String statusLabel,
        Long priceKobo,
        String swapWanted,
        String pickupLocation,
        String primaryImageUrl,
        int ownerGeneration,
        int viewCount,
        double co2SavedKg,
        UserSummary owner,
        boolean savedByViewer,
        Instant createdAt) {

    public static ListingCard from(Listing listing, boolean savedByViewer) {
        return new ListingCard(
                listing.getId(),
                listing.getReference(),
                listing.getTitle(),
                listing.getCategory().name(),
                listing.getCategory().label(),
                listing.getCategory().slug(),
                listing.getItemCondition().name(),
                listing.getItemCondition().label(),
                listing.getIntent().name(),
                listing.getIntent().label(),
                listing.getStatus().name(),
                listing.getStatus().label(),
                listing.getPriceKobo(),
                listing.getSwapWanted(),
                listing.getPickupLocation(),
                listing.getImageUrls().isEmpty() ? null : listing.getImageUrls().get(0),
                listing.getOwnerGeneration(),
                listing.getViewCount(),
                listing.estimatedCo2SavedKg(),
                UserSummary.from(listing.getOwner()),
                savedByViewer,
                listing.getCreatedAt());
    }
}
