package ng.edu.unilag.tradeup.web.dto;

import java.util.List;

/** The numbers and queues on a students own dashboard. */
public record DashboardSummary(
        long activeListings,
        long reservedListings,
        long completedTrades,
        long unreadMessages,
        long pendingOffersReceived,
        double personalCo2SavedKg,
        List<ListingCard> recentListings,
        List<OfferView> offersAwaitingYou) {}
