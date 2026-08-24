package com.undangan.online.controller;

import com.undangan.online.dto.ApiResponse;
import com.undangan.online.dto.CreateThemeRequest;
import com.undangan.online.dto.ThemeDto;
import com.undangan.online.dto.ThemeListDto;
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
    public ResponseEntity<ApiResponse<List<ThemeListDto>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(themeManagementService.listAll(status, category, search)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ThemeDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(themeManagementService.getById(id)));
    }

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ThemeDto>> create(
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
        ThemeDto created = themeManagementService.create(request, zipFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tema berhasil diupload", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ThemeDto>> update(@PathVariable Long id,
                                            @Valid @RequestBody UpdateThemeRequest request) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok("Theme berhasil diupdate", themeManagementService.update(id, request, null)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ThemeDto>> updateStatus(@PathVariable Long id,
                                                   @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(ApiResponse.ok("Status theme berhasil diupdate", themeManagementService.updateStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        themeManagementService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Theme berhasil dihapus", null));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.ok(themeManagementService.listCategories()));
    }

    @PatchMapping(value = "/{id}/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ThemeDto>> uploadZip(@PathVariable Long id,
                                                @RequestParam("zipFile") MultipartFile zipFile) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok("Tema berhasil diupload", themeManagementService.uploadZip(id, zipFile)));
    }
}
