package ng.edu.unilag.tradeup.web.error;

import org.springframework.http.HttpStatus;

/** Signed in, but not allowed to touch this particular resource. */
public class AccessDeniedAppException extends AppException {

    public AccessDeniedAppException(String message) {
        super(HttpStatus.FORBIDDEN, "forbidden", message);
    }
}
