package com.undangan.online.controller;

import com.undangan.online.dto.ThemeDto;
import com.undangan.online.service.ThemeManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/client/themes")
@PreAuthorize("hasRole('USER')")
public class ClientThemeController {

    private final ThemeManagementService themeManagementService;

    public ClientThemeController(ThemeManagementService themeManagementService) {
        this.themeManagementService = themeManagementService;
    }

    @GetMapping
    public ResponseEntity<java.util.List<com.undangan.online.dto.ThemeListDto>> listAvailable() {
        return ResponseEntity.ok(themeManagementService.listActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThemeDto> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(themeManagementService.getActiveById(id));
    }

    @PostMapping("/invitation/theme")
    public ResponseEntity<Map<String, String>> assignTheme(
            @RequestBody Map<String, Long> body) {
        Long themeId = body.get("themeId");
        Long clientId = getCurrentClientId();
        themeManagementService.assignToMyInvitation(clientId, themeId);
        return ResponseEntity.ok(Map.of("success", "true", "message", "Tema berhasil dipilih"));
    }

    @GetMapping("/invitation/theme")
    public ResponseEntity<ThemeDto> getCurrentTheme() {
        Long clientId = getCurrentClientId();
        ThemeDto theme = themeManagementService.getCurrentTheme(clientId);
        return ResponseEntity.ok(theme);
    }

    @DeleteMapping("/invitation/theme")
    public ResponseEntity<Void> removeTheme() {
        Long clientId = getCurrentClientId();
        themeManagementService.removeThemeFromInvitation(clientId);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentClientId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new com.undangan.online.exception.AuthException("UNAUTHORIZED", "Tidak terautentikasi", 401);
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof com.undangan.online.entity.Users user) {
            if (user.getClientId() == null) {
                throw new com.undangan.online.exception.AuthException("FORBIDDEN", "User bukan client", 403);
            }
            return user.getClientId();
        }
        throw new com.undangan.online.exception.AuthException("UNAUTHORIZED", "Tidak terautentikasi", 401);
    }
}
