package ng.edu.unilag.tradeup.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** One message posted into a thread. */
public record SendMessageRequest(
        @NotBlank(message = "Type a message first")
        @Size(max = 2000, message = "Messages are limited to 2000 characters")
        String body) {}
