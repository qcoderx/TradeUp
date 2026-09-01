package ng.edu.unilag.tradeup.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import ng.edu.unilag.tradeup.domain.Category;
import ng.edu.unilag.tradeup.domain.Listing;
import ng.edu.unilag.tradeup.domain.ListingStatus;
import ng.edu.unilag.tradeup.domain.Offer;
import ng.edu.unilag.tradeup.domain.SavedListing;
import ng.edu.unilag.tradeup.domain.TradeIntent;
import ng.edu.unilag.tradeup.domain.User;
import ng.edu.unilag.tradeup.repository.ListingRepository;
import ng.edu.unilag.tradeup.repository.ListingSpecifications;
import ng.edu.unilag.tradeup.repository.OfferRepository;
import ng.edu.unilag.tradeup.repository.SavedListingRepository;
import ng.edu.unilag.tradeup.web.dto.ListingCard;
import ng.edu.unilag.tradeup.web.dto.ListingDetail;
import ng.edu.unilag.tradeup.web.dto.ListingRequest;
import ng.edu.unilag.tradeup.web.dto.PageResponse;
import ng.edu.unilag.tradeup.web.error.AccessDeniedAppException;
import ng.edu.unilag.tradeup.web.error.NotFoundException;
import ng.edu.unilag.tradeup.web.error.ValidationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Everything that happens to a listing: creating it, finding it, editing it,
 * and moving it through its lifecycle.
 *
 * <p>The state changes themselves are deliberately not implemented here. They
 * live on {@link Listing}, so this class is responsible for permissions and
 * persistence while the entity stays responsible for what is legal.
 */
@Service
public class ListingService {

    private static final int MAX_PAGE_SIZE = 48;

    private final ListingRepository listingRepository;
    private final SavedListingRepository savedListingRepository;
    private final OfferRepository offerRepository;
    private final ReferenceGenerator referenceGenerator;

    public ListingService(
            ListingRepository listingRepository,
            SavedListingRepository savedListingRepository,
            OfferRepository offerRepository,
            ReferenceGenerator referenceGenerator) {
        this.listingRepository = listingRepository;
        this.savedListingRepository = savedListingRepository;
        this.offerRepository = offerRepository;
        this.referenceGenerator = referenceGenerator;
    }

    // ---------------------------------------------------------------------
    // Reading
    // ---------------------------------------------------------------------

    /**
     * The browse screen. Folds whichever filters the student used into one
     * query and marks each card with whether the viewer has saved it.
     */
    @Transactional(readOnly = true)
    public PageResponse<ListingCard> search(ListingSearchQuery query, Optional<User> viewer) {
        Specification<Listing> spec = ListingSpecifications.allOf(
                ListingSpecifications.publiclyVisible(),
                ListingSpecifications.matchesKeyword(query.keyword()),
                ListingSpecifications.inCategory(query.category()),
                ListingSpecifications.hasCondition(query.conditions()),
                ListingSpecifications.hasIntent(query.intent()),
                ListingSpecifications.priceAtLeast(query.minPriceKobo()),
                ListingSpecifications.priceAtMost(query.maxPriceKobo()));

        Pageable pageable = PageRequest.of(
                Math.max(0, query.page()),
                Math.clamp(query.size(), 1, MAX_PAGE_SIZE),
                query.sort().toSort());

        Page<Listing> results = listingRepository.findAll(spec, pageable);
        Set<Long> saved = savedIdsFor(viewer);
        return PageResponse.from(results, listing -> ListingCard.from(listing, saved.contains(listing.getId())));
    }

    /**
     * The item page. Viewing also records a view, which is why this is not a
     * read-only transaction.
     */
    @Transactional
    public ListingDetail findDetail(Long id, Optional<User> viewer) {
        Listing listing = requireListing(id);

        boolean ownedByViewer = viewer.map(listing::isOwnedBy).orElse(false);

        // A listing that is not public is only visible to its owner or a moderator.
        if (!listing.getStatus().isPubliclyVisible() && !ownedByViewer && !isModerator(viewer)) {
            throw NotFoundException.of("Listing", id);
        }

        if (!ownedByViewer) {
            listing.recordView();
        }

        boolean savedByViewer = viewer
                .map(user -> savedListingRepository.findByUserIdAndListingId(user.getId(), id).isPresent())
                .orElse(false);

        return ListingDetail.from(
                listing,
                ownedByViewer,
                savedByViewer,
                savedListingRepository.countByListingId(id),
                offerRepository.countByListingIdAndStatus(id, Offer.Status.PENDING));
    }

    @Transactional(readOnly = true)
    public ListingCard findByReference(String reference, Optional<User> viewer) {
        Listing listing = listingRepository
                .findByReferenceIgnoreCase(reference)
                .orElseThrow(() -> new NotFoundException("No listing has the code " + reference + "."));
        return ListingCard.from(listing, savedIdsFor(viewer).contains(listing.getId()));
    }

    /** Newest arrivals for the landing page. */
    @Transactional(readOnly = true)
    public List<ListingCard> latest(int limit, Optional<User> viewer) {
        Set<Long> saved = savedIdsFor(viewer);
        return listingRepository
                .findByStatusOrderByCreatedAtDesc(ListingStatus.ACTIVE, PageRequest.of(0, Math.clamp(limit, 1, 24)))
                .stream()
                .map(listing -> ListingCard.from(listing, saved.contains(listing.getId())))
                .toList();
    }

    /** Most-viewed available items. */
    @Transactional(readOnly = true)
    public List<ListingCard> trending(int limit, Optional<User> viewer) {
        Set<Long> saved = savedIdsFor(viewer);
        return listingRepository
                .findByStatusOrderByViewCountDesc(ListingStatus.ACTIVE, PageRequest.of(0, Math.clamp(limit, 1, 24)))
                .stream()
                .map(listing -> ListingCard.from(listing, saved.contains(listing.getId())))
                .toList();
    }

    /** Other items in the same category, for the bottom of the detail page. */
    @Transactional(readOnly = true)
    public List<ListingCard> similarTo(Long listingId, int limit, Optional<User> viewer) {
        Listing listing = requireListing(listingId);
        Set<Long> saved = savedIdsFor(viewer);
        return listingRepository
                .findByCategoryAndStatusAndIdNot(
                        listing.getCategory(),
                        ListingStatus.ACTIVE,
                        listingId,
                        PageRequest.of(0, Math.clamp(limit, 1, 12)))
                .stream()
                .map(other -> ListingCard.from(other, saved.contains(other.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingCard> listingsOwnedBy(Long ownerId, int page, int size, Optional<User> viewer) {
        Set<Long> saved = savedIdsFor(viewer);
        Page<Listing> results = listingRepository.findByOwnerIdOrderByCreatedAtDesc(
                ownerId, PageRequest.of(Math.max(0, page), Math.clamp(size, 1, MAX_PAGE_SIZE)));
        return PageResponse.from(results, listing -> ListingCard.from(listing, saved.contains(listing.getId())));
    }

    /** The public profile view: only what is currently on offer. */
    @Transactional(readOnly = true)
    public List<ListingCard> activeListingsOf(Long ownerId, Optional<User> viewer) {
        Set<Long> saved = savedIdsFor(viewer);
        return listingRepository.findByOwnerIdAndStatus(ownerId, ListingStatus.ACTIVE).stream()
                .map(listing -> ListingCard.from(listing, saved.contains(listing.getId())))
                .toList();
    }

    // ---------------------------------------------------------------------
    // Writing
    // ---------------------------------------------------------------------

    @Transactional
    public ListingDetail create(User owner, ListingRequest request) {
        validateIntentAgainstTerms(request);

        Listing listing = new Listing(
                referenceGenerator.next(listingRepository::existsByReference),
                owner,
                request.title().trim(),
                request.description().trim(),
                request.category(),
                request.itemCondition(),
                request.intent());

        applyTradeTerms(listing, request);
        listing.setPickupLocation(trimToNull(request.pickupLocation()));
        listing.setImageUrls(request.imageUrls());

        Listing saved = listingRepository.save(listing);
        return ListingDetail.from(saved, true, false, 0, 0);
    }

    @Transactional
    public ListingDetail update(Long id, User editor, ListingRequest request) {
        Listing listing = requireOwnedListing(id, editor);
        validateIntentAgainstTerms(request);

        if (listing.getStatus().isTerminal()) {
            throw new ValidationException("This listing is closed and can no longer be edited.");
        }

        listing.setTitle(request.title().trim());
        listing.setDescription(request.description().trim());
        listing.setCategory(request.category());
        listing.setItemCondition(request.itemCondition());
        listing.setIntent(request.intent());
        applyTradeTerms(listing, request);
        listing.setPickupLocation(trimToNull(request.pickupLocation()));
        listing.setImageUrls(request.imageUrls());

        return ListingDetail.from(
                listing,
                true,
                false,
                savedListingRepository.countByListingId(id),
                offerRepository.countByListingIdAndStatus(id, Offer.Status.PENDING));
    }

    @Transactional
    public void reserve(Long id, User owner) {
        requireOwnedListing(id, owner).reserve();
    }

    @Transactional
    public void release(Long id, User owner) {
        requireOwnedListing(id, owner).release();
    }

    /** The handover happened. Any offers still open on the item are closed out. */
    @Transactional
    public void complete(Long id, User owner) {
        Listing listing = requireOwnedListing(id, owner);
        listing.complete();
        offerRepository.findByListingIdOrderByCreatedAtDesc(id).stream()
                .filter(offer -> offer.getStatus() == Offer.Status.PENDING)
                .forEach(Offer::decline);
    }

    /**
     * Takes a listing down. The row is kept rather than deleted so that the
     * conversations and offers attached to it still make sense afterwards.
     */
    @Transactional
    public void remove(Long id, User requester) {
        Listing listing = requireListing(id);
        if (!listing.isOwnedBy(requester) && !requester.isAdmin()) {
            throw new AccessDeniedAppException("You can only remove your own listings.");
        }
        listing.remove();
    }

    // ---------------------------------------------------------------------
    // Saved items
    // ---------------------------------------------------------------------

    /**
     * Saves or unsaves a listing.
     *
     * @return true if the listing is saved after this call
     */
    @Transactional
    public boolean toggleSaved(Long listingId, User user) {
        Listing listing = requireListing(listingId);
        if (listing.isOwnedBy(user)) {
            throw new ValidationException("You cannot save your own listing.");
        }

        Optional<SavedListing> existing = savedListingRepository.findByUserIdAndListingId(user.getId(), listingId);
        if (existing.isPresent()) {
            savedListingRepository.delete(existing.get());
            return false;
        }
        savedListingRepository.save(new SavedListing(user, listing));
        return true;
    }

    @Transactional(readOnly = true)
    public List<ListingCard> savedListingsOf(User user) {
        return savedListingRepository.findAllForUser(user.getId()).stream()
                .map(saved -> ListingCard.from(saved.getListing(), true))
                .toList();
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    /** Loads a listing or reports it missing. */
    Listing requireListing(Long id) {
        return listingRepository.findById(id).orElseThrow(() -> NotFoundException.of("Listing", id));
    }

    private Listing requireOwnedListing(Long id, User user) {
        Listing listing = requireListing(id);
        if (!listing.isOwnedBy(user)) {
            throw new AccessDeniedAppException("This listing belongs to another student.");
        }
        return listing;
    }

    /**
     * The cross-field rule bean validation cannot express: an item for sale needs
     * a price, and an item for swap needs to say what it is looking for.
     */
    private void validateIntentAgainstTerms(ListingRequest request) {
        TradeIntent intent = request.intent();
        if (intent.acceptsCash() && request.priceKobo() == null) {
            throw new ValidationException("Set a price, or list the item for swap only.");
        }
        if (intent == TradeIntent.SWAP && (request.swapWanted() == null || request.swapWanted().isBlank())) {
            throw new ValidationException("Say what you would like in exchange.");
        }
    }

    /** Clears the terms that do not apply to the chosen intent. */
    private void applyTradeTerms(Listing listing, ListingRequest request) {
        listing.setPriceKobo(request.intent().acceptsCash() ? request.priceKobo() : null);
        listing.setSwapWanted(request.intent().acceptsSwap() ? trimToNull(request.swapWanted()) : null);
    }

    /** The set of listing ids this viewer has saved, empty for a signed-out visitor. */
    private Set<Long> savedIdsFor(Optional<User> viewer) {
        return viewer.map(user -> new HashSet<>(savedListingRepository.findSavedListingIds(user.getId())))
                .map(ids -> (Set<Long>) ids)
                .orElseGet(Set::of);
    }

    private boolean isModerator(Optional<User> viewer) {
        return viewer.map(User::isAdmin).orElse(false);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Category counts for the browse sidebar, in a fixed display order. */
    @Transactional(readOnly = true)
    public List<Object[]> availableCountsByCategory() {
        List<Object[]> rows = new ArrayList<>(listingRepository.countByCategory(ListingStatus.ACTIVE));
        rows.sort((a, b) -> ((Category) a[0]).ordinal() - ((Category) b[0]).ordinal());
        return rows;
    }
}
