package ng.edu.unilag.tradeup.service;

import ng.edu.unilag.tradeup.domain.User;
import ng.edu.unilag.tradeup.repository.UserRepository;
import ng.edu.unilag.tradeup.security.JwtService;
import ng.edu.unilag.tradeup.web.dto.AuthResponse;
import ng.edu.unilag.tradeup.web.dto.LoginRequest;
import ng.edu.unilag.tradeup.web.dto.RegisterRequest;
import ng.edu.unilag.tradeup.web.dto.UpdateProfileRequest;
import ng.edu.unilag.tradeup.web.dto.UserSummary;
import ng.edu.unilag.tradeup.web.error.AuthenticationFailedException;
import ng.edu.unilag.tradeup.web.error.DuplicateResourceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration, sign-in, and edits to a students own account. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Creates an account and signs the student straight in.
     *
     * <p>The duplicate checks run before the insert so the student gets a
     * sentence naming the field that clashed, rather than a database constraint
     * error that means nothing to them.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String matric = request.matricNumber().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account already uses that email address.");
        }
        if (userRepository.existsByMatricNumberIgnoreCase(matric)) {
            throw new DuplicateResourceException("An account already uses that matric number.");
        }

        User user = new User(
                email,
                matric,
                request.fullName().trim(),
                passwordEncoder.encode(request.password()),
                blankToNull(request.department()));
        user.setCampusLocation(blankToNull(request.campusLocation()));

        return tokenFor(userRepository.save(user));
    }

    /**
     * Signs a student in with either their email or their matric number.
     *
     * <p>Both the unknown-account and wrong-password paths return the same
     * message, so the form cannot be used to work out which email addresses are
     * registered.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        User user = userRepository
                .findByEmailIgnoreCase(identifier)
                .or(() -> userRepository.findByMatricNumberIgnoreCase(identifier))
                .orElseThrow(() -> new AuthenticationFailedException("Those details did not match an account."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("Those details did not match an account.");
        }
        if (!user.isActive()) {
            throw new AuthenticationFailedException("This account has been suspended.");
        }

        return tokenFor(user);
    }

    @Transactional
    public UserSummary updateProfile(User user, UpdateProfileRequest request) {
        User managed = userRepository.findById(user.getId()).orElseThrow();
        managed.setFullName(request.fullName().trim());
        managed.setDepartment(blankToNull(request.department()));
        managed.setCampusLocation(blankToNull(request.campusLocation()));
        managed.setBio(blankToNull(request.bio()));
        return UserSummary.from(userRepository.save(managed));
    }

    private AuthResponse tokenFor(User user) {
        return new AuthResponse(
                jwtService.issueToken(user), jwtService.validity().toSeconds(), UserSummary.from(user));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
