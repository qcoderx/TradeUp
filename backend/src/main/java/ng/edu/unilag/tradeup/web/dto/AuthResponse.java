package ng.edu.unilag.tradeup.web.dto;

/** A freshly issued token and the account it belongs to. */
public record AuthResponse(String token, long expiresInSeconds, UserSummary user) {}
