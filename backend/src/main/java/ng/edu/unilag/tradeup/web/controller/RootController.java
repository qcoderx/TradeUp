package ng.edu.unilag.tradeup.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The service root.
 *
 * <p>Exists so that something cheap, public and obvious answers at {@code /}.
 * An uptime monitor pinging a free host to stop it sleeping should not have to
 * know an internal path, and should certainly not be told to sign in.
 *
 * <p>It deliberately touches no database. A keep-alive ping runs every few
 * minutes forever, so it must not cost a query — and it should keep answering
 * even if the database is unreachable, because the web tier being up is exactly
 * what the monitor is asking about. Spring answers HEAD from this mapping too,
 * which is what most uptime services send.
 */
@RestController
@Tag(name = "Service", description = "Liveness and service metadata")
public class RootController {

    @GetMapping({"/", "/ping"})
    @Operation(summary = "Liveness check for uptime monitors")
    public Map<String, Object> root() {
        return Map.of(
                "service", "TradeUp API",
                "status", "ok",
                "project", "University of Lagos, COS202 Group 15",
                "docs", "/swagger-ui.html",
                "time", Instant.now().toString());
    }
}
