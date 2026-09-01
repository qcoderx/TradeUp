package ng.edu.unilag.tradeup.security;

import java.util.Collection;
import java.util.List;
import ng.edu.unilag.tradeup.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adapts a TradeUp {@link User} to what Spring Security expects, while keeping
 * the domain object reachable so controllers can use it directly.
 */
public class AppUserPrincipal implements UserDetails {

    private final transient User user;

    public AppUserPrincipal(User user) {
        this.user = user;
    }

    public User user() {
        return user;
    }

    public Long id() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
