package ng.edu.unilag.tradeup.web.dto;

import jakarta.validation.constraints.Size;

/** A moderators verdict on a report. */
public record ModerationDecision(
        @Size(max = 500, message = "Keep the note under 500 characters") String note) {}
