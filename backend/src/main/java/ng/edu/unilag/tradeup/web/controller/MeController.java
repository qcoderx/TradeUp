package ng.edu.unilag.tradeup.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import ng.edu.unilag.tradeup.domain.User;
import ng.edu.unilag.tradeup.security.CurrentUser;
import ng.edu.unilag.tradeup.service.DashboardService;
import ng.edu.unilag.tradeup.service.ListingService;
import ng.edu.unilag.tradeup.service.OfferService;
import ng.edu.unilag.tradeup.web.dto.DashboardSummary;
import ng.edu.unilag.tradeup.web.dto.ListingCard;
import ng.edu.unilag.tradeup.web.dto.OfferView;
import ng.edu.unilag.tradeup.web.dto.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Everything scoped to the signed-in student. */
@RestController
@RequestMapping("/api/me")
@Tag(name = "My account", description = "Your dashboard, listings, saved items and offers")
public class MeController {

    private final DashboardService dashboardService;
    private final ListingService listingService;
    private final OfferService offerService;
    private final CurrentUser currentUser;

    public MeController(
            DashboardService dashboardService,
            ListingService listingService,
            OfferService offerService,
            CurrentUser currentUser) {
        this.dashboardService = dashboardService;
        this.listingService = listingService;
        this.offerService = offerService;
        this.currentUser = currentUser;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Counts, impact and the offers waiting on you")
    public DashboardSummary dashboard() {
        return dashboardService.forUser(currentUser.require());
    }

    @GetMapping("/listings")
    @Operation(summary = "Everything you have listed, in any status")
    public PageResponse<ListingCard> myListings(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "24") int size) {
        User user = currentUser.require();
        return listingService.listingsOwnedBy(user.getId(), page, size, Optional.of(user));
    }

    @GetMapping("/saved")
    @Operation(summary = "Items you have saved")
    public List<ListingCard> saved() {
        return listingService.savedListingsOf(currentUser.require());
    }

    @GetMapping("/offers/made")
    @Operation(summary = "Offers you have made on other items")
    public List<OfferView> offersMade() {
        return offerService.made(currentUser.require());
    }

    @GetMapping("/offers/received")
    @Operation(summary = "Offers waiting on your decision")
    public List<OfferView> offersReceived() {
        return offerService.awaiting(currentUser.require());
    }
}
