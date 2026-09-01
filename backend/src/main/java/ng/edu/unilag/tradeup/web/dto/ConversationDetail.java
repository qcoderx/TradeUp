package ng.edu.unilag.tradeup.web.dto;

import java.util.List;

/** A full thread with the listing it is about. */
public record ConversationDetail(
        Long id, ListingCard listing, UserSummary counterpart, List<MessageView> messages) {}
