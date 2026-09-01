package ng.edu.unilag.tradeup.security;

import java.util.Optional;
import ng.edu.unilag.tradeup.domain.User;
import ng.edu.unilag.tradeup.web.error.AuthenticationFailedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Reads the signed-in student out of the security context.
 *
 * <p>Endpoints that are open to everyone but behave differently when signed in
 * (browse, for instance, marks which items you have saved) use
 * {@link #optional()}; endpoints that require an account use {@link #require()}.
 */
@Component
public class CurrentUser {

    /** The signed-in student, or empty for an anonymous visitor. */
    public Optional<User> optional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            return Optional.of(principal.user());
        }
        return Optional.empty();
    }

    /** The signed-in student, or a 401 if there is not one. */
    public User require() {
        return optional().orElseThrow(() -> new AuthenticationFailedException("Sign in to continue."));
    }

    public Optional<Long> optionalId() {
        return optional().map(User::getId);
    }
}
