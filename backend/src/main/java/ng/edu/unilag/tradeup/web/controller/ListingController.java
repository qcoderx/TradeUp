package ng.edu.unilag.tradeup.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import ng.edu.unilag.tradeup.domain.Category;
import ng.edu.unilag.tradeup.domain.Condition;
import ng.edu.unilag.tradeup.domain.TradeIntent;
import ng.edu.unilag.tradeup.security.CurrentUser;
import ng.edu.unilag.tradeup.service.ListingSearchQuery;
import ng.edu.unilag.tradeup.service.ListingService;
import ng.edu.unilag.tradeup.service.ModerationService;
import ng.edu.unilag.tradeup.service.OfferService;
import ng.edu.unilag.tradeup.web.dto.ListingCard;
import ng.edu.unilag.tradeup.web.dto.ListingDetail;
import ng.edu.unilag.tradeup.web.dto.ListingRequest;
import ng.edu.unilag.tradeup.web.dto.OfferRequest;
import ng.edu.unilag.tradeup.web.dto.OfferView;
import ng.edu.unilag.tradeup.web.dto.PageResponse;
import ng.edu.unilag.tradeup.web.dto.ReportRequest;
import ng.edu.unilag.tradeup.web.dto.ReportView;
import ng.edu.unilag.tradeup.web.error.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The marketplace itself.
 *
 * <p>Reading is open to anyone; every write requires a token. The optional
 * viewer is threaded through the read endpoints so a signed-in student sees
 * which items they have already saved without a second request.
 */
@RestController
@RequestMapping("/api/listings")
@Tag(name = "Listings", description = "Browsing, creating and managing items")
public class ListingController {

    private final ListingService listingService;
    private final OfferService offerService;
    private final ModerationService moderationService;
    private final CurrentUser currentUser;

    public ListingController(
            ListingService listingService,
            OfferService offerService,
            ModerationService moderationService,
            CurrentUser currentUser) {
        this.listingService = listingService;
        this.offerService = offerService;
        this.moderationService = moderationService;
        this.currentUser = currentUser;
    }

    // ---------------------------------------------------------------------
    // Browsing
    // ---------------------------------------------------------------------

    @GetMapping
    @Operation(summary = "Search and filter available items")
    public PageResponse<ListingCard> browse(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<Condition> condition,
            @RequestParam(required = false) TradeIntent intent,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false, defaultValue = "NEWEST") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {

        Category resolved = resolveCategory(category);
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new ValidationException("The lowest price cannot be more than the highest.");
        }

        ListingSearchQuery query = new ListingSearchQuery(
                q,
                resolved,
                condition,
                intent,
                toKobo(minPrice),
                toKobo(maxPrice),
                ListingSearchQuery.SortOption.parse(sort),
                page,
                size);

        return listingService.search(query, currentUser.optional());
    }

    @GetMapping("/latest")
    @Operation(summary = "Newest arrivals")
    public List<ListingCard> latest(@RequestParam(defaultValue = "8") int limit) {
        return listingService.latest(limit, currentUser.optional());
    }

    @GetMapping("/trending")
    @Operation(summary = "Most-viewed available items")
    public List<ListingCard> trending(@RequestParam(defaultValue = "8") int limit) {
        return listingService.trending(limit, currentUser.optional());
    }

    @GetMapping("/{id}")
    @Operation(summary = "One item in full")
    public ListingDetail detail(@PathVariable Long id) {
        return listingService.findDetail(id, currentUser.optional());
    }

    @GetMapping("/{id}/similar")
    @Operation(summary = "Other items in the same category")
    public List<ListingCard> similar(@PathVariable Long id, @RequestParam(defaultValue = "4") int limit) {
        return listingService.similarTo(id, limit, currentUser.optional());
    }

    @GetMapping("/by-reference/{reference}")
    @Operation(summary = "Look an item up by the code on its ticket")
    public ListingCard byReference(@PathVariable String reference) {
        return listingService.findByReference(reference, currentUser.optional());
    }

    // ---------------------------------------------------------------------
    // Managing your own listings
    // ---------------------------------------------------------------------

    @PostMapping
    @Operation(summary = "List an item")
    public ResponseEntity<ListingDetail> create(@Valid @RequestBody ListingRequest request) {
        ListingDetail created = listingService.create(currentUser.require(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit one of your listings")
    public ListingDetail update(@PathVariable Long id, @Valid @RequestBody ListingRequest request) {
        return listingService.update(id, currentUser.require(), request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Take one of your listings down")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        listingService.remove(id, currentUser.require());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reserve")
    @Operation(summary = "Hold an item for someone")
    public ResponseEntity<Void> reserve(@PathVariable Long id) {
        listingService.reserve(id, currentUser.require());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Put a reserved item back on the board")
    public ResponseEntity<Void> release(@PathVariable Long id) {
        listingService.release(id, currentUser.require());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Confirm the handover happened")
    public ResponseEntity<Void> complete(@PathVariable Long id) {
        listingService.complete(id, currentUser.require());
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------
    // Saving, offering, reporting
    // ---------------------------------------------------------------------

    @PostMapping("/{id}/save")
    @Operation(summary = "Save or unsave an item")
    public Map<String, Boolean> toggleSaved(@PathVariable Long id) {
        return Map.of("saved", listingService.toggleSaved(id, currentUser.require()));
    }

    @PostMapping("/{id}/offers")
    @Operation(summary = "Offer cash or a swap on an item")
    public ResponseEntity<OfferView> makeOffer(@PathVariable Long id, @Valid @RequestBody OfferRequest request) {
        OfferView offer = offerService.make(id, currentUser.require(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(offer);
    }

    @GetMapping("/{id}/offers")
    @Operation(summary = "Offers received on your listing")
    public List<OfferView> offers(@PathVariable Long id) {
        return offerService.onListing(id, currentUser.require());
    }

    @PostMapping("/{id}/report")
    @Operation(summary = "Flag a listing for a moderator")
    public ResponseEntity<ReportView> report(@PathVariable Long id, @Valid @RequestBody ReportRequest request) {
        ReportView report = moderationService.report(id, currentUser.require(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Accepts either a slug ({@code lab-equipment}) or an enum name. */
    private Category resolveCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Category.fromSlug(raw)
                .orElseThrow(() -> new ValidationException("There is no category called " + raw + "."));
    }

    /** The API takes prices in naira; everything inside works in kobo. */
    private static Long toKobo(Long naira) {
        return naira == null ? null : naira * 100;
    }
}
