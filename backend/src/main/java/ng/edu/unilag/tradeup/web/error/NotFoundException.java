package ng.edu.unilag.tradeup.web.error;

import org.springframework.http.HttpStatus;

/** The thing being asked for is not in the database. */
public class NotFoundException extends AppException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "not_found", message);
    }

    /** Convenience for the common "Listing 42 could not be found." shape. */
    public static NotFoundException of(String entity, Object identifier) {
        return new NotFoundException("%s %s could not be found.".formatted(entity, identifier));
    }
}
