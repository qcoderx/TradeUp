package ng.edu.unilag.tradeup.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import ng.edu.unilag.tradeup.security.CurrentUser;
import ng.edu.unilag.tradeup.service.StorageService;
import ng.edu.unilag.tradeup.web.error.ValidationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Listing photo uploads. */
@RestController
@RequestMapping("/api/uploads")
@Tag(name = "Uploads", description = "Listing photos")
public class UploadController {

    private static final int MAX_FILES_PER_REQUEST = 6;

    private final StorageService storageService;
    private final CurrentUser currentUser;

    public UploadController(StorageService storageService, CurrentUser currentUser) {
        this.storageService = storageService;
        this.currentUser = currentUser;
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Upload up to six listing photos")
    public Map<String, List<String>> upload(@RequestParam("files") List<MultipartFile> files) {
        currentUser.require();

        if (files == null || files.isEmpty()) {
            throw new ValidationException("Choose at least one image.");
        }
        if (files.size() > MAX_FILES_PER_REQUEST) {
            throw new ValidationException("You can upload up to " + MAX_FILES_PER_REQUEST + " photos at a time.");
        }

        return Map.of("urls", files.stream().map(storageService::store).toList());
    }
}
