package ng.edu.unilag.tradeup.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** What a new student submits to create an account. */
public record RegisterRequest(
        @NotBlank(message = "Enter your full name")
        @Size(max = 120, message = "That name is too long")
        String fullName,

        @NotBlank(message = "Enter your email")
        @Email(message = "Enter a valid email address")
        @Size(max = 120)
        String email,

        @NotBlank(message = "Enter your matric number")
        @Pattern(regexp = "^[0-9]{9}$", message = "A matric number is 9 digits, e.g. 240817017")
        String matricNumber,

        @NotBlank(message = "Choose a password")
        @Size(min = 8, max = 72, message = "Use at least 8 characters")
        String password,

        @Size(max = 80)
        String department,

        @Size(max = 80)
        String campusLocation) {}
