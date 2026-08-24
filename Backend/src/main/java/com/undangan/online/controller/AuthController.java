package com.undangan.online.controller;

import com.undangan.online.dto.ApiResponse;
import com.undangan.online.dto.LoginRequest;
import com.undangan.online.dto.LoginResponse;
import com.undangan.online.dto.RegisterClientRequest;
import com.undangan.online.dto.UserDto;
import com.undangan.online.service.AuthService;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.ok("Login berhasil", response));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterClientRequest request) {
        UserDto user = authService.registerClient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User berhasil dibuat", user));
    }
}
