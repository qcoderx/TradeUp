package ng.edu.unilag.tradeup.web.error;

import org.springframework.http.HttpStatus;

/** The request is well formed but the object is in the wrong state for it. */
public class InvalidTransitionException extends AppException {

    public InvalidTransitionException(String message) {
        super(HttpStatus.CONFLICT, "invalid_transition", message);
    }
}
