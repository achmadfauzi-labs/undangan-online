package com.undangan.online.controller;

import com.undangan.online.dto.ApiResponse;
import com.undangan.online.dto.ThemeDto;
import com.undangan.online.dto.ThemeListDto;
import com.undangan.online.entity.Users;
import com.undangan.online.exception.AuthException;
import com.undangan.online.service.ThemeManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/client/themes")
@PreAuthorize("hasRole('USER')")
public class ClientThemeController {

    private final ThemeManagementService themeManagementService;

    public ClientThemeController(ThemeManagementService themeManagementService) {
        this.themeManagementService = themeManagementService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ThemeListDto>>> listAvailable() {
        return ResponseEntity.ok(ApiResponse.ok(themeManagementService.listActive()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ThemeDto>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(themeManagementService.getActiveById(id)));
    }

    @PostMapping("/invitation/theme")
    public ResponseEntity<ApiResponse<Void>> assignTheme(
            @RequestBody java.util.Map<String, Long> body) {
        Long themeId = body.get("themeId");
        Long clientId = getCurrentClientId();
        themeManagementService.assignToMyInvitation(clientId, themeId);
        return ResponseEntity.ok(ApiResponse.ok("Tema berhasil dipilih", null));
    }

    @GetMapping("/invitation/theme")
    public ResponseEntity<ApiResponse<ThemeDto>> getCurrentTheme() {
        Long clientId = getCurrentClientId();
        ThemeDto theme = themeManagementService.getCurrentTheme(clientId);
        return ResponseEntity.ok(ApiResponse.ok(theme));
    }

    @DeleteMapping("/invitation/theme")
    public ResponseEntity<ApiResponse<Void>> removeTheme() {
        Long clientId = getCurrentClientId();
        themeManagementService.removeThemeFromInvitation(clientId);
        return ResponseEntity.ok(ApiResponse.ok("Tema berhasil dihapus dari undangan", null));
    }

    private Long getCurrentClientId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthException("UNAUTHORIZED", "Tidak terautentikasi", 401);
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Users user) {
            if (user.getClientId() == null) {
                throw new AuthException("FORBIDDEN", "User bukan client", 403);
            }
            return user.getClientId();
        }
        throw new AuthException("UNAUTHORIZED", "Tidak terautentikasi", 401);
    }
}
