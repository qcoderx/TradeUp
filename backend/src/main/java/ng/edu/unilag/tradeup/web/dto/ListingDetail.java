package ng.edu.unilag.tradeup.web.dto;

import java.time.Instant;
import java.util.List;
import ng.edu.unilag.tradeup.domain.Listing;

/** The full item page, including everything the card leaves out. */
public record ListingDetail(
        Long id,
        String reference,
        String title,
        String description,
        String categoryName,
        String categoryLabel,
        String categorySlug,
        String conditionName,
        String conditionLabel,
        String conditionDescription,
        String intentName,
        String intentLabel,
        boolean acceptsCash,
        boolean acceptsSwap,
        String statusName,
        String statusLabel,
        Long priceKobo,
        String swapWanted,
        String pickupLocation,
        List<String> imageUrls,
        int ownerGeneration,
        int viewCount,
        double co2SavedKg,
        UserSummary owner,
        boolean ownedByViewer,
        boolean savedByViewer,
        long savedCount,
        long pendingOfferCount,
        Instant createdAt,
        Instant updatedAt) {

    public static ListingDetail from(
            Listing listing,
            boolean ownedByViewer,
            boolean savedByViewer,
            long savedCount,
            long pendingOfferCount) {
        return new ListingDetail(
                listing.getId(),
                listing.getReference(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getCategory().name(),
                listing.getCategory().label(),
                listing.getCategory().slug(),
                listing.getItemCondition().name(),
                listing.getItemCondition().label(),
                listing.getItemCondition().description(),
                listing.getIntent().name(),
                listing.getIntent().label(),
                listing.getIntent().acceptsCash(),
                listing.getIntent().acceptsSwap(),
                listing.getStatus().name(),
                listing.getStatus().label(),
                listing.getPriceKobo(),
                listing.getSwapWanted(),
                listing.getPickupLocation(),
                List.copyOf(listing.getImageUrls()),
                listing.getOwnerGeneration(),
                listing.getViewCount(),
                listing.estimatedCo2SavedKg(),
                UserSummary.from(listing.getOwner()),
                ownedByViewer,
                savedByViewer,
                savedCount,
                pendingOfferCount,
                listing.getCreatedAt(),
                listing.getUpdatedAt());
    }
}
