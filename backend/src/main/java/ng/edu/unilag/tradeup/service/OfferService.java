package ng.edu.unilag.tradeup.service;

import java.util.List;
import ng.edu.unilag.tradeup.domain.Listing;
import ng.edu.unilag.tradeup.domain.Offer;
import ng.edu.unilag.tradeup.domain.User;
import ng.edu.unilag.tradeup.repository.OfferRepository;
import ng.edu.unilag.tradeup.web.dto.OfferRequest;
import ng.edu.unilag.tradeup.web.dto.OfferView;
import ng.edu.unilag.tradeup.web.error.AccessDeniedAppException;
import ng.edu.unilag.tradeup.web.error.NotFoundException;
import ng.edu.unilag.tradeup.web.error.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cash and swap proposals on listings.
 *
 * <p>Accepting an offer reserves the item rather than completing it: the trade
 * is only finished once the two students have actually met and handed the thing
 * over, which the owner confirms separately.
 */
@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final ListingService listingService;

    public OfferService(OfferRepository offerRepository, ListingService listingService) {
        this.offerRepository = offerRepository;
        this.listingService = listingService;
    }

    @Transactional
    public OfferView make(Long listingId, User offeredBy, OfferRequest request) {
        Listing listing = listingService.requireListing(listingId);

        if (listing.isOwnedBy(offeredBy)) {
            throw new ValidationException("You cannot make an offer on your own listing.");
        }
        if (!listing.getStatus().isPubliclyVisible()) {
            throw new ValidationException("This item is not accepting offers right now.");
        }
        if (offerRepository.existsByListingIdAndOfferedByIdAndStatus(listingId, offeredBy.getId(), Offer.Status.PENDING)) {
            throw new ValidationException("You already have an offer waiting on this item.");
        }

        Offer offer =
                switch (request.kind()) {
                    case CASH -> buildCashOffer(listing, offeredBy, request);
                    case SWAP -> buildSwapOffer(listing, offeredBy, request);
                };

        return OfferView.from(offerRepository.save(offer));
    }

    private Offer buildCashOffer(Listing listing, User offeredBy, OfferRequest request) {
        if (!listing.getIntent().acceptsCash()) {
            throw new ValidationException("This item is listed for swap only.");
        }
        if (request.amountKobo() == null) {
            throw new ValidationException("Enter how much you are offering.");
        }
        return Offer.cash(listing, offeredBy, request.amountKobo(), request.note());
    }

    private Offer buildSwapOffer(Listing listing, User offeredBy, OfferRequest request) {
        if (!listing.getIntent().acceptsSwap()) {
            throw new ValidationException("This item is listed for sale only.");
        }
        if (request.offeredListingId() == null) {
            throw new ValidationException("Pick one of your items to offer in exchange.");
        }

        Listing offered = listingService.requireListing(request.offeredListingId());
        if (!offered.isOwnedBy(offeredBy)) {
            throw new AccessDeniedAppException("You can only offer an item you own.");
        }
        if (!offered.getStatus().isPubliclyVisible()) {
            throw new ValidationException("The item you are offering is not available.");
        }
        return Offer.swap(listing, offeredBy, offered, request.note());
    }

    /** Offers made on this students listings that still need a decision. */
    @Transactional(readOnly = true)
    public List<OfferView> awaiting(User owner) {
        return offerRepository
                .findByListingOwnerIdAndStatusOrderByCreatedAtDesc(owner.getId(), Offer.Status.PENDING)
                .stream()
                .map(OfferView::from)
                .toList();
    }

    /** Offers this student has made on other peoples listings. */
    @Transactional(readOnly = true)
    public List<OfferView> made(User user) {
        return offerRepository.findByOfferedByIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(OfferView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OfferView> onListing(Long listingId, User owner) {
        Listing listing = listingService.requireListing(listingId);
        if (!listing.isOwnedBy(owner)) {
            throw new AccessDeniedAppException("Only the lister can see offers on this item.");
        }
        return offerRepository.findByListingIdOrderByCreatedAtDesc(listingId).stream()
                .map(OfferView::from)
                .toList();
    }

    /**
     * Accepts an offer and holds the item for that student. Every other pending
     * offer on the same listing is declined in the same transaction, so nobody
     * is left waiting on something that is already spoken for.
     */
    @Transactional
    public OfferView accept(Long offerId, User owner) {
        Offer offer = requireOffer(offerId);
        requireListingOwner(offer, owner);

        offer.accept();

        offerRepository.findByListingIdOrderByCreatedAtDesc(offer.getListing().getId()).stream()
                .filter(other -> !other.getId().equals(offerId))
                .filter(other -> other.getStatus() == Offer.Status.PENDING)
                .forEach(Offer::decline);

        Listing listing = offer.getListing();
        if (listing.getStatus() == ng.edu.unilag.tradeup.domain.ListingStatus.ACTIVE) {
            listing.reserve();
        }

        return OfferView.from(offer);
    }

    @Transactional
    public OfferView decline(Long offerId, User owner) {
        Offer offer = requireOffer(offerId);
        requireListingOwner(offer, owner);
        offer.decline();
        return OfferView.from(offer);
    }

    @Transactional
    public OfferView withdraw(Long offerId, User offeredBy) {
        Offer offer = requireOffer(offerId);
        if (!offer.getOfferedBy().getId().equals(offeredBy.getId())) {
            throw new AccessDeniedAppException("You can only withdraw your own offer.");
        }
        offer.withdraw();
        return OfferView.from(offer);
    }

    private void requireListingOwner(Offer offer, User user) {
        if (!offer.getListing().isOwnedBy(user)) {
            throw new AccessDeniedAppException("Only the lister can respond to this offer.");
        }
    }

    private Offer requireOffer(Long id) {
        return offerRepository.findById(id).orElseThrow(() -> NotFoundException.of("Offer", id));
    }
}
