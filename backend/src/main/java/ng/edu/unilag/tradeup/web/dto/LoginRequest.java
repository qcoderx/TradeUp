package ng.edu.unilag.tradeup.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Credentials for signing in. Email or matric number both work. */
public record LoginRequest(
        @NotBlank(message = "Enter your email or matric number") String identifier,
        @NotBlank(message = "Enter your password") String password) {}
