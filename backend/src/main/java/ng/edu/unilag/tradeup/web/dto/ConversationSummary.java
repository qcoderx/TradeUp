package ng.edu.unilag.tradeup.web.dto;

import java.time.Instant;

/** One row in the inbox. */
public record ConversationSummary(
        Long id,
        Long listingId,
        String listingTitle,
        String listingReference,
        String listingImageUrl,
        UserSummary counterpart,
        String lastMessagePreview,
        Instant lastMessageAt,
        long unreadCount) {}
