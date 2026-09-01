package ng.edu.unilag.tradeup.service;

import java.util.List;
import java.util.Optional;
import ng.edu.unilag.tradeup.domain.ListingStatus;
import ng.edu.unilag.tradeup.domain.User;
import ng.edu.unilag.tradeup.repository.ListingRepository;
import ng.edu.unilag.tradeup.web.dto.DashboardSummary;
import ng.edu.unilag.tradeup.web.dto.ListingCard;
import ng.edu.unilag.tradeup.web.dto.OfferView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Assembles the one call that fills a students dashboard. */
@Service
public class DashboardService {

    private static final int RECENT_LISTING_COUNT = 6;

    private final ListingRepository listingRepository;
    private final ListingService listingService;
    private final MessagingService messagingService;
    private final OfferService offerService;
    private final ImpactService impactService;

    public DashboardService(
            ListingRepository listingRepository,
            ListingService listingService,
            MessagingService messagingService,
            OfferService offerService,
            ImpactService impactService) {
        this.listingRepository = listingRepository;
        this.listingService = listingService;
        this.messagingService = messagingService;
        this.offerService = offerService;
        this.impactService = impactService;
    }

    @Transactional(readOnly = true)
    public DashboardSummary forUser(User user) {
        List<OfferView> awaiting = offerService.awaiting(user);

        List<ListingCard> recent = listingService
                .listingsOwnedBy(user.getId(), 0, RECENT_LISTING_COUNT, Optional.of(user))
                .items();

        return new DashboardSummary(
                countOwn(user, ListingStatus.ACTIVE),
                countOwn(user, ListingStatus.RESERVED),
                countOwn(user, ListingStatus.COMPLETED),
                messagingService.unreadCount(user),
                awaiting.size(),
                impactService.personalCo2SavedKg(user),
                recent,
                awaiting);
    }

    private long countOwn(User user, ListingStatus status) {
        return listingRepository.findByOwnerIdAndStatus(user.getId(), status).size();
    }
}
