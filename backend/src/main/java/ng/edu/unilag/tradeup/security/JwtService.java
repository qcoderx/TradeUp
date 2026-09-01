package ng.edu.unilag.tradeup.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import ng.edu.unilag.tradeup.domain.User;
import ng.edu.unilag.tradeup.web.error.AuthenticationFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies the signed tokens that stand in for a session.
 *
 * <p>TradeUp is stateless: the server keeps no session table, so every request
 * carries its own proof of identity. The signing key comes from configuration
 * and must be overridden in any real deployment.
 */
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final Duration validity;
    private final String issuer;

    public JwtService(
            @Value("${tradeup.security.jwt-secret}") String secret,
            @Value("${tradeup.security.token-validity:P7D}") Duration validity,
            @Value("${tradeup.security.issuer:tradeup}") String issuer) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "tradeup.security.jwt-secret must be at least 32 characters so HS256 has enough entropy.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.validity = validity;
        this.issuer = issuer;
    }

    /** Mints a token identifying this user for {@code tradeup.security.token-validity}. */
    public String issueToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_NAME, user.getFullName())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifies the signature and expiry, returning the user id in the subject.
     *
     * @throws AuthenticationFailedException if the token is expired, tampered
     *     with, or otherwise unreadable
     */
    public Long extractUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AuthenticationFailedException("Your session is no longer valid. Please sign in again.");
        }
    }

    /** How long an issued token lasts, so the client can schedule a refresh. */
    public Duration validity() {
        return validity;
    }
}
