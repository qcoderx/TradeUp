package ng.edu.unilag.tradeup.service;

import java.util.List;
import ng.edu.unilag.tradeup.domain.Conversation;
import ng.edu.unilag.tradeup.domain.Listing;
import ng.edu.unilag.tradeup.domain.Message;
import ng.edu.unilag.tradeup.domain.User;
import ng.edu.unilag.tradeup.repository.ConversationRepository;
import ng.edu.unilag.tradeup.web.dto.ConversationDetail;
import ng.edu.unilag.tradeup.web.dto.ConversationSummary;
import ng.edu.unilag.tradeup.web.dto.ListingCard;
import ng.edu.unilag.tradeup.web.dto.MessageView;
import ng.edu.unilag.tradeup.web.dto.UserSummary;
import ng.edu.unilag.tradeup.web.error.NotFoundException;
import ng.edu.unilag.tradeup.web.error.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The private threads between a student interested in an item and the student
 * who listed it.
 *
 * <p>Participation checks live on {@link Conversation} itself, so there is no
 * way to reach a thread through this service without passing them.
 */
@Service
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final ListingService listingService;

    public MessagingService(ConversationRepository conversationRepository, ListingService listingService) {
        this.conversationRepository = conversationRepository;
        this.listingService = listingService;
    }

    /**
     * Opens the thread about a listing, creating it on first contact.
     *
     * <p>Reusing an existing thread is what keeps the inbox to one row per item
     * instead of a new thread for every question.
     */
    @Transactional
    public ConversationDetail startOrResume(Long listingId, User buyer, String openingMessage) {
        Listing listing = listingService.requireListing(listingId);

        if (listing.isOwnedBy(buyer)) {
            throw new ValidationException("This is your own listing.");
        }
        if (listing.getStatus().isTerminal()) {
            throw new ValidationException("This item is no longer available.");
        }

        Conversation conversation = conversationRepository
                .findByListingIdAndBuyerId(listingId, buyer.getId())
                .orElseGet(() -> conversationRepository.save(new Conversation(listing, buyer)));

        if (openingMessage != null && !openingMessage.isBlank()) {
            conversation.post(buyer, openingMessage.trim());
        }

        conversationRepository.save(conversation);
        return toDetail(conversation, buyer);
    }

    @Transactional
    public MessageView post(Long conversationId, User sender, String body) {
        Conversation conversation = requireConversation(conversationId);
        conversation.requireParticipant(sender);

        if (conversation.getListing().getStatus().isTerminal()) {
            throw new ValidationException("This item is no longer available.");
        }

        Message message = conversation.post(sender, body.trim());
        conversationRepository.save(conversation);
        return MessageView.from(message, sender.getId());
    }

    /** The inbox, newest activity first. */
    @Transactional(readOnly = true)
    public List<ConversationSummary> inbox(User user) {
        return conversationRepository.findAllForUser(user.getId()).stream()
                .map(conversation -> toSummary(conversation, user))
                .toList();
    }

    /** Opening a thread marks the other side messages as read. */
    @Transactional
    public ConversationDetail open(Long conversationId, User reader) {
        Conversation conversation = requireConversation(conversationId);
        conversation.requireParticipant(reader);
        conversation.markReadBy(reader);
        conversationRepository.save(conversation);
        return toDetail(conversation, reader);
    }

    /** Total unread messages across every thread, for the navbar badge. */
    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return conversationRepository.findAllForUser(user.getId()).stream()
                .mapToLong(conversation -> conversation.unreadCountFor(user))
                .sum();
    }

    private ConversationSummary toSummary(Conversation conversation, User viewer) {
        Listing listing = conversation.getListing();
        return new ConversationSummary(
                conversation.getId(),
                listing.getId(),
                listing.getTitle(),
                listing.getReference(),
                listing.getImageUrls().isEmpty() ? null : listing.getImageUrls().get(0),
                UserSummary.from(conversation.counterpartOf(viewer)),
                conversation.latestMessage().map(Message::getBody).orElse(null),
                conversation.latestMessage().map(Message::getCreatedAt).orElse(conversation.getCreatedAt()),
                conversation.unreadCountFor(viewer));
    }

    private ConversationDetail toDetail(Conversation conversation, User viewer) {
        return new ConversationDetail(
                conversation.getId(),
                ListingCard.from(conversation.getListing(), false),
                UserSummary.from(conversation.counterpartOf(viewer)),
                conversation.getMessages().stream()
                        .map(message -> MessageView.from(message, viewer.getId()))
                        .toList());
    }

    private Conversation requireConversation(Long id) {
        return conversationRepository.findById(id).orElseThrow(() -> NotFoundException.of("Conversation", id));
    }
}
