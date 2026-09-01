package ng.edu.unilag.tradeup.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import ng.edu.unilag.tradeup.domain.User;
import ng.edu.unilag.tradeup.repository.UserRepository;
import ng.edu.unilag.tradeup.security.CurrentUser;
import ng.edu.unilag.tradeup.service.ImpactService;
import ng.edu.unilag.tradeup.service.ListingService;
import ng.edu.unilag.tradeup.service.ReferenceService;
import ng.edu.unilag.tradeup.service.TeamService;
import ng.edu.unilag.tradeup.web.dto.ImpactSnapshot;
import ng.edu.unilag.tradeup.web.dto.ReferenceData;
import ng.edu.unilag.tradeup.web.dto.TeamMember;
import ng.edu.unilag.tradeup.web.dto.UserProfile;
import ng.edu.unilag.tradeup.web.error.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The endpoints a visitor can reach without an account: the vocabularies that
 * drive the filters, the SDG 12 figures, public profiles, and the group behind
 * the project.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Public", description = "Reference data, impact figures and the project team")
public class PublicController {

    private final ReferenceService referenceService;
    private final ImpactService impactService;
    private final TeamService teamService;
    private final ListingService listingService;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public PublicController(
            ReferenceService referenceService,
            ImpactService impactService,
            TeamService teamService,
            ListingService listingService,
            UserRepository userRepository,
            CurrentUser currentUser) {
        this.referenceService = referenceService;
        this.impactService = impactService;
        this.teamService = teamService;
        this.listingService = listingService;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @GetMapping("/reference")
    @Operation(summary = "Categories, conditions and trade intents with their labels")
    public ReferenceData reference() {
        return referenceService.load();
    }

    @GetMapping("/impact")
    @Operation(summary = "What the marketplace has kept in circulation so far")
    public ImpactSnapshot impact() {
        return impactService.snapshot();
    }

    @GetMapping("/team")
    @Operation(summary = "Group 15, COS202, University of Lagos")
    public List<TeamMember> team() {
        return teamService.members();
    }

    @GetMapping("/users/{id}/profile")
    @Operation(summary = "A students public profile and what they have listed")
    public UserProfile profile(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> NotFoundException.of("Student", id));
        return UserProfile.from(user, listingService.activeListingsOf(id, currentUser.optional()));
    }
}
