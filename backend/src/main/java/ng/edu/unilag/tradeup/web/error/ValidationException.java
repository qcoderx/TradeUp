package ng.edu.unilag.tradeup.web.error;

import org.springframework.http.HttpStatus;

/** A rule that bean validation cannot express on its own was broken. */
public class ValidationException extends AppException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "invalid_request", message);
    }
}
