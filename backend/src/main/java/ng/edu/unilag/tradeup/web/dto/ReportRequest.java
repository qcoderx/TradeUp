package ng.edu.unilag.tradeup.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ng.edu.unilag.tradeup.domain.Report;

/** Flagging a listing for a moderator. */
public record ReportRequest(
        @NotNull(message = "Choose a reason") Report.Reason reason,
        @Size(max = 700, message = "Keep the details under 700 characters") String details) {}
