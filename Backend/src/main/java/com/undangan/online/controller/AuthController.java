package com.undangan.online.controller;

import com.undangan.online.entity.Users;
import com.undangan.online.dto.ApiResponse;
import com.undangan.online.dto.LoginRequest;
import com.undangan.online.dto.LoginResponse;
import com.undangan.online.dto.RefreshRequest;
import com.undangan.online.dto.RefreshResponse;
import com.undangan.online.dto.RegisterClientRequest;
import com.undangan.online.dto.UserDto;
import com.undangan.online.service.AuthService;
import com.undangan.online.service.RefreshTokenService;
import com.undangan.online.util.IpAddressUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = IpAddressUtil.extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        LoginResponse response = authService.login(request.getUsername(), request.getPassword(), ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.ok("Login berhasil", response));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterClientRequest request) {
        UserDto user = authService.registerClient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User berhasil dibuat", user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        String ipAddress = IpAddressUtil.extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String newRefreshToken = refreshTokenService.rotateRefreshToken(request.getRefreshToken(), userAgent, ipAddress);
        Users user = refreshTokenService.validateRefreshToken(newRefreshToken);
        String accessToken = authService.generateAccessToken(user);
        RefreshResponse response = new RefreshResponse(accessToken, newRefreshToken, "Bearer", authService.getAccessTokenExpiresIn());
        return ResponseEntity.ok(ApiResponse.ok("Token berhasil diperbarui", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.deleteRefreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Logout berhasil", null));
    }
}
