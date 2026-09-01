package ng.edu.unilag.tradeup.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ng.edu.unilag.tradeup.security.CurrentUser;
import ng.edu.unilag.tradeup.service.OfferService;
import ng.edu.unilag.tradeup.web.dto.OfferView;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responding to an offer. Making one lives on the listing it is about. */
@RestController
@RequestMapping("/api/offers")
@Tag(name = "Offers", description = "Accepting, declining and withdrawing offers")
public class OfferController {

    private final OfferService offerService;
    private final CurrentUser currentUser;

    public OfferController(OfferService offerService, CurrentUser currentUser) {
        this.offerService = offerService;
        this.currentUser = currentUser;
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept an offer and hold the item for that student")
    public OfferView accept(@PathVariable Long id) {
        return offerService.accept(id, currentUser.require());
    }

    @PostMapping("/{id}/decline")
    @Operation(summary = "Turn an offer down")
    public OfferView decline(@PathVariable Long id) {
        return offerService.decline(id, currentUser.require());
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Take back an offer you made")
    public OfferView withdraw(@PathVariable Long id) {
        return offerService.withdraw(id, currentUser.require());
    }
}
