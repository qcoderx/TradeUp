package ng.edu.unilag.tradeup.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ng.edu.unilag.tradeup.domain.Offer;

/**
 * A proposal on someone elses listing. Supply {@code amountKobo} for a CASH
 * offer or {@code offeredListingId} for a SWAP; the service rejects the wrong
 * combination.
 */
public record OfferRequest(
        @NotNull(message = "Choose whether you are offering cash or a swap") Offer.Kind kind,
        @Positive(message = "Enter an amount greater than zero") Long amountKobo,
        Long offeredListingId,
        @Size(max = 500, message = "Keep your note under 500 characters") String note) {}
