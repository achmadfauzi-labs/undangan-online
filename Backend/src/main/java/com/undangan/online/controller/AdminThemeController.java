package com.undangan.online.controller;

import com.undangan.online.dto.CreateThemeRequest;
import com.undangan.online.dto.ThemeDto;
import com.undangan.online.dto.UpdateThemeRequest;
import com.undangan.online.service.ThemeManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/themes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminThemeController {

    private final ThemeManagementService themeManagementService;

    public AdminThemeController(ThemeManagementService themeManagementService) {
        this.themeManagementService = themeManagementService;
    }

    @GetMapping
    public ResponseEntity<List<com.undangan.online.dto.ThemeListDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(themeManagementService.listAll(status, category, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThemeDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(themeManagementService.getById(id));
    }

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ThemeDto> create(
            @RequestParam("code") String code,
            @RequestParam("name") String name,
            @RequestParam("category") String category,
            @RequestParam(value = "thumbnailCss", required = false) String thumbnailCss,
            @RequestParam(value = "zipFile", required = false) MultipartFile zipFile) throws Exception {
        CreateThemeRequest request = new CreateThemeRequest();
        request.setCode(code);
        request.setName(name);
        request.setCategory(category);
        request.setThumbnailCss(thumbnailCss);
        return ResponseEntity.status(HttpStatus.CREATED).body(themeManagementService.create(request, zipFile));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ThemeDto> update(@PathVariable Long id,
                                           @Valid @RequestBody UpdateThemeRequest request) throws IOException {
        return ResponseEntity.ok(themeManagementService.update(id, request, null));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ThemeDto> updateStatus(@PathVariable Long id,
                                                  @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(themeManagementService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        themeManagementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> listCategories() {
        return ResponseEntity.ok(themeManagementService.listCategories());
    }

    @PatchMapping(value = "/{id}/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ThemeDto> uploadZip(@PathVariable Long id,
                                               @RequestParam("zipFile") MultipartFile zipFile) throws Exception {
        return ResponseEntity.ok(themeManagementService.uploadZip(id, zipFile));
    }
}
