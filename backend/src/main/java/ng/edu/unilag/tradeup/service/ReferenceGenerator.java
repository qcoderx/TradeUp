package ng.edu.unilag.tradeup.service;

import java.security.SecureRandom;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * Produces the short codes printed on each trade ticket, e.g. {@code TU-7QK42}.
 *
 * <p>The alphabet leaves out I, O, 0 and 1 so a code read off a phone screen and
 * typed into the search box cannot be ambiguous.
 */
@Component
public class ReferenceGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 5;
    private static final int MAX_ATTEMPTS = 25;

    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a code that {@code isTaken} says is free.
     *
     * @throws IllegalStateException if the space is so crowded that repeated
     *     attempts all collide, which means it is time for a longer code
     */
    public String next(Predicate<String> isTaken) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = generate();
            if (!isTaken.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a free listing reference after " + MAX_ATTEMPTS + " tries.");
    }

    private String generate() {
        StringBuilder code = new StringBuilder("TU-");
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
