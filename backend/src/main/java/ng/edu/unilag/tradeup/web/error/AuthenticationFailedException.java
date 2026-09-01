package ng.edu.unilag.tradeup.web.error;

import org.springframework.http.HttpStatus;

/** Bad credentials, or a token that is missing, expired, or malformed. */
public class AuthenticationFailedException extends AppException {

    public AuthenticationFailedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "unauthenticated", message);
    }
}
