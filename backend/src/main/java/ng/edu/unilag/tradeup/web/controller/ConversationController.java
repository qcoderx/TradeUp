package ng.edu.unilag.tradeup.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import ng.edu.unilag.tradeup.security.CurrentUser;
import ng.edu.unilag.tradeup.service.MessagingService;
import ng.edu.unilag.tradeup.web.dto.ConversationDetail;
import ng.edu.unilag.tradeup.web.dto.ConversationSummary;
import ng.edu.unilag.tradeup.web.dto.MessageView;
import ng.edu.unilag.tradeup.web.dto.SendMessageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Private threads between a buyer and a lister. */
@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Messages", description = "Talking to the student on the other side of a trade")
public class ConversationController {

    private final MessagingService messagingService;
    private final CurrentUser currentUser;

    public ConversationController(MessagingService messagingService, CurrentUser currentUser) {
        this.messagingService = messagingService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "Your inbox")
    public List<ConversationSummary> inbox() {
        return messagingService.inbox(currentUser.require());
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Total unread messages, for the navbar badge")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", messagingService.unreadCount(currentUser.require()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Open a thread and mark it read")
    public ConversationDetail open(@PathVariable Long id) {
        return messagingService.open(id, currentUser.require());
    }

    @PostMapping("/listing/{listingId}")
    @Operation(summary = "Start or resume the thread about an item")
    public ResponseEntity<ConversationDetail> startOrResume(
            @PathVariable Long listingId, @Valid @RequestBody SendMessageRequest request) {
        ConversationDetail detail =
                messagingService.startOrResume(listingId, currentUser.require(), request.body());
        return ResponseEntity.status(HttpStatus.CREATED).body(detail);
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Reply in a thread")
    public ResponseEntity<MessageView> reply(
            @PathVariable Long id, @Valid @RequestBody SendMessageRequest request) {
        MessageView message = messagingService.post(id, currentUser.require(), request.body());
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
}
