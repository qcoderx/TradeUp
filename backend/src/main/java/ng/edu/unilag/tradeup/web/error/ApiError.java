package ng.edu.unilag.tradeup.web.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/**
 * The single error shape every failed request returns, so the frontend only ever
 * has to understand one thing.
 *
 * @param code stable machine-readable identifier, e.g. {@code not_found}
 * @param message sentence safe to show directly to a student
 * @param fieldErrors per-field problems for form validation, omitted when empty
 * @param timestamp when the failure happened
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String code, String message, Map<String, String> fieldErrors, Instant timestamp) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Map.of(), Instant.now());
    }

    public static ApiError of(String code, String message, Map<String, String> fieldErrors) {
        return new ApiError(code, message, fieldErrors, Instant.now());
    }
}
