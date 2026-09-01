package ng.edu.unilag.tradeup.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ng.edu.unilag.tradeup.security.CurrentUser;
import ng.edu.unilag.tradeup.service.AuthService;
import ng.edu.unilag.tradeup.web.dto.AuthResponse;
import ng.edu.unilag.tradeup.web.dto.LoginRequest;
import ng.edu.unilag.tradeup.web.dto.RegisterRequest;
import ng.edu.unilag.tradeup.web.dto.UpdateProfileRequest;
import ng.edu.unilag.tradeup.web.dto.UserSummary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Creating an account and signing in")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    @Operation(summary = "Create a student account and sign in")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in with an email or matric number")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "The account behind the current token")
    public UserSummary me() {
        return UserSummary.from(currentUser.require());
    }

    @PutMapping("/me")
    @Operation(summary = "Update your own profile")
    public UserSummary updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(currentUser.require(), request);
    }
}
