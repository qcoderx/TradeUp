package ng.edu.unilag.tradeup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/** A single message inside a {@link Conversation}. */
@Entity
@Table(name = "messages")
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 2000)
    private String body;

    private Instant readAt;

    protected Message() {
        // Required by JPA.
    }

    Message(Conversation conversation, User sender, String body) {
        this.conversation = Objects.requireNonNull(conversation, "conversation");
        this.sender = Objects.requireNonNull(sender, "sender");
        this.body = body;
    }

    public boolean isRead() {
        return readAt != null;
    }

    void markRead() {
        if (readAt == null) {
            this.readAt = Instant.now();
        }
    }

    public Conversation getConversation() {
        return conversation;
    }

    public User getSender() {
        return sender;
    }

    public String getBody() {
        return body;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
