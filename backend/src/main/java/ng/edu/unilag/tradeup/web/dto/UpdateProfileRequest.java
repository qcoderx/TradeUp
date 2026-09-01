package ng.edu.unilag.tradeup.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Editable parts of a students own profile. */
public record UpdateProfileRequest(
        @NotBlank(message = "Enter your full name") @Size(max = 120) String fullName,
        @Size(max = 80) String department,
        @Size(max = 80) String campusLocation,
        @Size(max = 400, message = "Keep your bio under 400 characters") String bio) {}
