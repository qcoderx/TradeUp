package ng.edu.unilag.tradeup.web.dto;

import java.time.Instant;
import ng.edu.unilag.tradeup.domain.Offer;

/** An offer as shown in the dashboard, on either side of the trade. */
public record OfferView(
        Long id,
        String kind,
        String status,
        Long amountKobo,
        String note,
        UserSummary offeredBy,
        Long listingId,
        String listingTitle,
        String listingReference,
        String listingImageUrl,
        Long offeredListingId,
        String offeredListingTitle,
        String offeredListingImageUrl,
        Instant createdAt) {

    public static OfferView from(Offer offer) {
        var listing = offer.getListing();
        var swapped = offer.getOfferedListing();
        return new OfferView(
                offer.getId(),
                offer.getKind().name(),
                offer.getStatus().name(),
                offer.getAmountKobo(),
                offer.getNote(),
                UserSummary.from(offer.getOfferedBy()),
                listing.getId(),
                listing.getTitle(),
                listing.getReference(),
                listing.getImageUrls().isEmpty() ? null : listing.getImageUrls().get(0),
                swapped == null ? null : swapped.getId(),
                swapped == null ? null : swapped.getTitle(),
                swapped == null || swapped.getImageUrls().isEmpty() ? null : swapped.getImageUrls().get(0),
                offer.getCreatedAt());
    }
}
