package ng.edu.unilag.tradeup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import ng.edu.unilag.tradeup.domain.Listing;
import ng.edu.unilag.tradeup.repository.ListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the catalogue out to CSV and JSON on disk.
 *
 * <p>This is the file-handling half of the proposal. The database is the live
 * store, but a marketplace that only exists inside an embedded database is hard
 * to hand to anyone: these snapshots give the group a portable record they can
 * open in a spreadsheet, diff between runs, or submit alongside the code.
 *
 * <p>All writing goes through {@link java.nio.file.Files}, and every stream is
 * opened in a try-with-resources so a failure part way through a large export
 * still closes the handle.
 */
@Service
public class ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final List<String> CSV_COLUMNS = List.of(
            "reference",
            "title",
            "category",
            "condition",
            "intent",
            "status",
            "price_naira",
            "swap_wanted",
            "pickup_location",
            "owner_generation",
            "views",
            "co2_saved_kg",
            "owner_name",
            "owner_matric",
            "created_at");

    private final ListingRepository listingRepository;
    private final ObjectMapper objectMapper;
    private final Path archiveDirectory;

    public ArchiveService(
            ListingRepository listingRepository,
            ObjectMapper objectMapper,
            @Value("${tradeup.storage.archive-dir}") Path archiveDirectory) {
        this.listingRepository = listingRepository;
        this.objectMapper = objectMapper;
        this.archiveDirectory = archiveDirectory;
    }

    /**
     * Writes a timestamped CSV of every listing.
     *
     * @return the file that was written
     */
    @Transactional(readOnly = true)
    public Path exportCsv() {
        Path target = prepare("listings-%s.csv".formatted(LocalDateTime.now().format(STAMP)));

        try (BufferedWriter writer = Files.newBufferedWriter(
                target, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            writer.write(String.join(",", CSV_COLUMNS));
            writer.newLine();

            for (Listing listing : listingRepository.findAll()) {
                writer.write(toCsvRow(listing));
                writer.newLine();
            }

            log.info("Exported listings to {}", target.toAbsolutePath());
            return target;

        } catch (IOException ex) {
            // Wrapped so callers are not forced to handle a checked exception for
            // something they cannot meaningfully recover from mid-request.
            throw new UncheckedIOException("Could not write the CSV export to " + target, ex);
        }
    }

    /** Writes the same data as JSON, for anything that needs the nested shape. */
    @Transactional(readOnly = true)
    public Path exportJson() {
        Path target = prepare("listings-%s.json".formatted(LocalDateTime.now().format(STAMP)));

        List<Map<String, Object>> rows = listingRepository.findAll().stream()
                .map(this::toMap)
                .toList();

        try (BufferedWriter writer = Files.newBufferedWriter(
                target, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, rows);
            log.info("Exported listings to {}", target.toAbsolutePath());
            return target;

        } catch (IOException ex) {
            throw new UncheckedIOException("Could not write the JSON export to " + target, ex);
        }
    }

    /** The exports written so far, newest first. */
    public List<String> listArchives() {
        if (!Files.isDirectory(archiveDirectory)) {
            return List.of();
        }
        try (var entries = Files.list(archiveDirectory)) {
            return entries.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted((a, b) -> b.compareTo(a))
                    .toList();
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read the archive directory.", ex);
        }
    }

    /** Reads one archive back off disk, for the download endpoint. */
    public byte[] read(String fileName) {
        Path target = archiveDirectory.resolve(fileName).normalize();

        // Refuse anything that escapes the archive directory, e.g. ../../secrets.
        if (!target.startsWith(archiveDirectory.normalize())) {
            throw new IllegalArgumentException("That file is outside the archive directory.");
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read " + fileName, ex);
        }
    }

    private Path prepare(String fileName) {
        try {
            Files.createDirectories(archiveDirectory);
            return archiveDirectory.resolve(fileName);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not create the archive directory " + archiveDirectory, ex);
        }
    }

    private String toCsvRow(Listing listing) {
        return String.join(
                ",",
                csv(listing.getReference()),
                csv(listing.getTitle()),
                csv(listing.getCategory().label()),
                csv(listing.getItemCondition().label()),
                csv(listing.getIntent().label()),
                csv(listing.getStatus().label()),
                csv(listing.getPriceKobo() == null ? "" : String.valueOf(listing.getPriceKobo() / 100.0)),
                csv(listing.getSwapWanted()),
                csv(listing.getPickupLocation()),
                csv(String.valueOf(listing.getOwnerGeneration())),
                csv(String.valueOf(listing.getViewCount())),
                csv(String.format("%.1f", listing.estimatedCo2SavedKg())),
                csv(listing.getOwner().getFullName()),
                csv(listing.getOwner().getMatricNumber()),
                csv(String.valueOf(listing.getCreatedAt())));
    }

    private Map<String, Object> toMap(Listing listing) {
        return Map.ofEntries(
                Map.entry("reference", listing.getReference()),
                Map.entry("title", listing.getTitle()),
                Map.entry("description", listing.getDescription()),
                Map.entry("category", listing.getCategory().label()),
                Map.entry("condition", listing.getItemCondition().label()),
                Map.entry("intent", listing.getIntent().label()),
                Map.entry("status", listing.getStatus().label()),
                Map.entry("priceNaira", listing.getPriceKobo() == null ? 0 : listing.getPriceKobo() / 100.0),
                Map.entry("swapWanted", listing.getSwapWanted() == null ? "" : listing.getSwapWanted()),
                Map.entry("ownerGeneration", listing.getOwnerGeneration()),
                Map.entry("views", listing.getViewCount()),
                Map.entry("co2SavedKg", listing.estimatedCo2SavedKg()),
                Map.entry("ownerName", listing.getOwner().getFullName()),
                Map.entry("ownerMatric", listing.getOwner().getMatricNumber()),
                Map.entry("createdAt", String.valueOf(listing.getCreatedAt())));
    }

    /**
     * Quotes a CSV field. A value containing a comma, quote or newline is wrapped
     * in quotes with any embedded quote doubled, which is what a spreadsheet
     * expects. Without this, a description with a comma in it would silently
     * shift every column after it.
     */
    private static String csv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (!needsQuoting) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
