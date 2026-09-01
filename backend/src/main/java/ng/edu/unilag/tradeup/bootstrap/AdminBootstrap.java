package ng.edu.unilag.tradeup.bootstrap;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import ng.edu.unilag.tradeup.domain.Role;
import ng.edu.unilag.tradeup.domain.User;
import ng.edu.unilag.tradeup.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grants moderator rights to named accounts at startup.
 *
 * <p>Moderation is deliberately not something you can sign up for, and there is
 * deliberately no shared moderator account with a password somebody has to pass
 * around. Instead the deployment names the people who should have it:
 *
 * <pre>TRADEUP_ADMIN_EMAILS=okiki.adebowale@live.unilag.edu.ng,deborah.bakare@...</pre>
 *
 * <p>Each of them registers normally, and is promoted the next time the
 * application starts. Removing an address demotes them again, so the environment
 * variable is the single source of truth rather than a row somebody edited by
 * hand months ago.
 */
@Component
@org.springframework.core.annotation.Order(2)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final List<String> adminEmails;

    public AdminBootstrap(
            UserRepository userRepository, @Value("${tradeup.security.admin-emails:}") String adminEmails) {
        this.userRepository = userRepository;
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .map(email -> email.toLowerCase(Locale.ROOT))
                .toList();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmails.isEmpty()) {
            log.info("No tradeup.security.admin-emails configured; nobody will be promoted to moderator.");
            return;
        }

        for (String email : adminEmails) {
            userRepository
                    .findByEmailIgnoreCase(email)
                    .ifPresentOrElse(
                            this::promote,
                            () -> log.warn(
                                    "{} is listed as a moderator but has no account yet. "
                                            + "They will be promoted once they register.",
                                    email));
        }

        demoteAnyoneNoLongerListed();
    }

    private void promote(User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        user.setRole(Role.ADMIN);
        userRepository.save(user);
        log.info("Promoted {} to moderator.", user.getEmail());
    }

    /**
     * Anyone holding ADMIN who is no longer named loses it. Without this, taking
     * someone off the list would quietly leave their access in place.
     */
    private void demoteAnyoneNoLongerListed() {
        userRepository.findAll().stream()
                .filter(User::isAdmin)
                .filter(user -> !adminEmails.contains(user.getEmail().toLowerCase(Locale.ROOT)))
                .forEach(user -> {
                    user.setRole(Role.STUDENT);
                    userRepository.save(user);
                    log.info("Removed moderator rights from {}; they are no longer listed.", user.getEmail());
                });
    }
}
