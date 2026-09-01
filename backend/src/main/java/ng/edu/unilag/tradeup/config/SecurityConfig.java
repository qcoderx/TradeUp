package ng.edu.unilag.tradeup.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import ng.edu.unilag.tradeup.security.JwtAuthenticationFilter;
import ng.edu.unilag.tradeup.web.error.ApiError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Who can reach what.
 *
 * <p>Browsing the marketplace is deliberately open: a first-year deciding
 * whether TradeUp is worth joining should be able to see real listings before
 * creating an account. Everything that writes, and everything that touches
 * another student, requires a token.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper,
            @Value("${tradeup.cors.allowed-origins}") List<String> allowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: signing up, signing in, and looking around.
                        .requestMatchers("/api/auth/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/listings", "/api/listings/*", "/api/listings/*/similar")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reference/**", "/api/impact", "/api/team")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/*/profile")
                        .permitAll()
                        // Uploaded listing photos and the generated brand assets.
                        .requestMatchers(HttpMethod.GET, "/uploads/**")
                        .permitAll()
                        // API documentation.
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers("/actuator/health")
                        .permitAll()
                        // The service root, so an uptime monitor keeping this
                        // instance awake gets a 200 rather than a 401. HEAD is
                        // listed explicitly because that is what most uptime
                        // services actually send.
                        .requestMatchers(HttpMethod.GET, "/", "/ping")
                        .permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/", "/ping")
                        .permitAll()
                        // Moderation is staff only.
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "unauthenticated",
                                        "Sign in to continue."))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "forbidden",
                                        "You do not have permission to do that.")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Security failures use the same JSON envelope as every other error. */
    private void writeError(HttpServletResponse response, int status, String code, String message) {
        try {
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ApiError.of(code, message));
        } catch (Exception ignored) {
            // The response is already committed; nothing useful left to do.
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** BCrypt at strength 12: slow enough to matter, fast enough for a login. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
