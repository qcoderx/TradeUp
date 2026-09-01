package ng.edu.unilag.tradeup.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import ng.edu.unilag.tradeup.domain.Category;
import ng.edu.unilag.tradeup.domain.Condition;
import ng.edu.unilag.tradeup.domain.TradeIntent;

/**
 * Creating or editing a listing.
 *
 * <p>Bean validation covers the shape of each field. The rule that ties them
 * together — a listing for sale needs a price, a listing for swap needs to say
 * what it wants — is enforced in {@code ListingService}, because it depends on
 * more than one field at once.
 */
public record ListingRequest(
        @NotBlank(message = "Give your item a title")
        @Size(min = 3, max = 120, message = "Titles run from 3 to 120 characters")
        String title,

        @NotBlank(message = "Describe the item so people know what they are getting")
        @Size(min = 10, max = 2000, message = "Descriptions run from 10 to 2000 characters")
        String description,

        @NotNull(message = "Pick a category") Category category,

        @NotNull(message = "Say what condition it is in") Condition itemCondition,

        @NotNull(message = "Choose whether this is for sale, for swap, or either") TradeIntent intent,

        @PositiveOrZero(message = "A price cannot be negative") Long priceKobo,

        @Size(max = 300, message = "Keep the swap note under 300 characters") String swapWanted,

        @Size(max = 80) String pickupLocation,

        @Size(max = 6, message = "You can add up to 6 photos") List<@NotBlank String> imageUrls) {}
