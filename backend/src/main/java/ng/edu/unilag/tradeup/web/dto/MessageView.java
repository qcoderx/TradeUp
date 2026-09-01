package ng.edu.unilag.tradeup.web.dto;

import java.time.Instant;
import ng.edu.unilag.tradeup.domain.Message;

/** A single message, flagged with whether the viewer wrote it. */
public record MessageView(
        Long id, String body, UserSummary sender, boolean mine, boolean read, Instant sentAt) {

    public static MessageView from(Message message, Long viewerId) {
        return new MessageView(
                message.getId(),
                message.getBody(),
                UserSummary.from(message.getSender()),
                message.getSender().getId().equals(viewerId),
                message.isRead(),
                message.getCreatedAt());
    }
}
