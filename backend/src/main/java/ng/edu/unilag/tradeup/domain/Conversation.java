package ng.edu.unilag.tradeup.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import ng.edu.unilag.tradeup.web.error.AccessDeniedAppException;

/**
 * A private thread between a buyer and a lister about one specific listing.
 *
 * <p>The unique constraint keeps a student from opening a second thread on an
 * item they are already talking about, so the inbox stays one row per item.
 */
@Entity
@Table(
        name = "conversations",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_conversation_listing_buyer",
                        columnNames = {"listing_id", "buyer_id"}))
public class Conversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    /** The student who started the conversation. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Message> messages = new ArrayList<>();

    protected Conversation() {
        // Required by JPA.
    }

    public Conversation(Listing listing, User buyer) {
        this.listing = Objects.requireNonNull(listing, "listing");
        this.buyer = Objects.requireNonNull(buyer, "buyer");
    }

    /** The lister is always the other side of the thread. */
    public User seller() {
        return listing.getOwner();
    }

    public boolean includes(User user) {
        // Fails closed on unsaved entities, whose ids are both null.
        if (user == null || user.getId() == null) {
            return false;
        }
        return user.getId().equals(buyer.getId()) || user.getId().equals(seller().getId());
    }

    /** Throws unless {@code user} is one of the two participants. */
    public void requireParticipant(User user) {
        if (!includes(user)) {
            throw new AccessDeniedAppException("This conversation belongs to someone else.");
        }
    }

    /** Given one participant, returns the person they are talking to. */
    public User counterpartOf(User user) {
        requireParticipant(user);
        return user.getId().equals(buyer.getId()) ? seller() : buyer;
    }

    /** Adds a message and keeps both sides of the association in step. */
    public Message post(User sender, String body) {
        requireParticipant(sender);
        Message message = new Message(this, sender, body);
        messages.add(message);
        return message;
    }

    public Optional<Message> latestMessage() {
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(messages.size() - 1));
    }

    /** How many messages in this thread {@code reader} has not opened yet. */
    public long unreadCountFor(User reader) {
        return messages.stream()
                .filter(m -> !Objects.equals(m.getSender().getId(), reader.getId()))
                .filter(m -> !m.isRead())
                .count();
    }

    /** Marks every message from the other side as read. */
    public void markReadBy(User reader) {
        requireParticipant(reader);
        messages.stream()
                .filter(m -> !Objects.equals(m.getSender().getId(), reader.getId()))
                .forEach(Message::markRead);
    }

    public Listing getListing() {
        return listing;
    }

    public User getBuyer() {
        return buyer;
    }

    public List<Message> getMessages() {
        return messages;
    }
}
