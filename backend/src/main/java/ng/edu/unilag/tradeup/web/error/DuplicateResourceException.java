package ng.edu.unilag.tradeup.web.error;

import org.springframework.http.HttpStatus;

/** Something unique already exists with these details. */
public class DuplicateResourceException extends AppException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, "duplicate", message);
    }
}
