package ng.edu.unilag.tradeup.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import ng.edu.unilag.tradeup.domain.Report;
import ng.edu.unilag.tradeup.security.CurrentUser;
import ng.edu.unilag.tradeup.service.ArchiveService;
import ng.edu.unilag.tradeup.service.ImpactService;
import ng.edu.unilag.tradeup.service.ModerationService;
import ng.edu.unilag.tradeup.web.dto.ModerationDecision;
import ng.edu.unilag.tradeup.web.dto.ReportView;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moderation and the data exports.
 *
 * <p>Every route under {@code /api/admin} requires the ADMIN role, enforced in
 * {@code SecurityConfig} rather than repeated on each method here.
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Moderation", description = "The report queue and catalogue exports")
public class AdminController {

    private final ModerationService moderationService;
    private final ArchiveService archiveService;
    private final ImpactService impactService;
    private final CurrentUser currentUser;

    public AdminController(
            ModerationService moderationService,
            ArchiveService archiveService,
            ImpactService impactService,
            CurrentUser currentUser) {
        this.moderationService = moderationService;
        this.archiveService = archiveService;
        this.impactService = impactService;
        this.currentUser = currentUser;
    }

    @GetMapping("/reports")
    @Operation(summary = "The moderation queue, filtered by status")
    public List<ReportView> reports(@RequestParam(defaultValue = "OPEN") Report.Status status) {
        return moderationService.queue(status);
    }

    @GetMapping("/overview")
    @Operation(summary = "Counts for the moderation dashboard")
    public Map<String, Object> overview() {
        return Map.of(
                "openReports", moderationService.openReportCount(),
                "impact", impactService.snapshot());
    }

    @PostMapping("/reports/{id}/uphold")
    @Operation(summary = "Agree with the report and pull the listing")
    public ReportView uphold(@PathVariable Long id, @Valid @RequestBody ModerationDecision decision) {
        return moderationService.uphold(id, currentUser.require(), decision.note());
    }

    @PostMapping("/reports/{id}/dismiss")
    @Operation(summary = "Dismiss the report and restore the listing")
    public ReportView dismiss(@PathVariable Long id, @Valid @RequestBody ModerationDecision decision) {
        return moderationService.dismiss(id, currentUser.require(), decision.note());
    }

    // ---------------------------------------------------------------------
    // File exports
    // ---------------------------------------------------------------------

    @PostMapping("/archives/csv")
    @Operation(summary = "Write the catalogue to a timestamped CSV")
    public Map<String, String> exportCsv() {
        Path written = archiveService.exportCsv();
        return Map.of("file", written.getFileName().toString());
    }

    @PostMapping("/archives/json")
    @Operation(summary = "Write the catalogue to a timestamped JSON file")
    public Map<String, String> exportJson() {
        Path written = archiveService.exportJson();
        return Map.of("file", written.getFileName().toString());
    }

    @GetMapping("/archives")
    @Operation(summary = "Exports written so far")
    public List<String> archives() {
        return archiveService.listArchives();
    }

    @GetMapping("/archives/{fileName}")
    @Operation(summary = "Download one export")
    public ResponseEntity<Resource> download(@PathVariable String fileName) {
        byte[] content = archiveService.read(fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(content));
    }
}
