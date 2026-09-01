package ng.edu.unilag.tradeup.repository;

import java.util.Optional;
import ng.edu.unilag.tradeup.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByMatricNumberIgnoreCase(String matricNumber);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByMatricNumberIgnoreCase(String matricNumber);
}
