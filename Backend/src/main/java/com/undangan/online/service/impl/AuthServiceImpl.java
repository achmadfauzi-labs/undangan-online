package com.undangan.online.service.impl;

import com.undangan.online.config.JwtConfig;
import com.undangan.online.config.PasswordPolicyConfig;
import com.undangan.online.dto.LoginResponse;
import com.undangan.online.dto.RegisterClientRequest;
import com.undangan.online.dto.UserDto;
import com.undangan.online.entity.Client;
import com.undangan.online.entity.Users;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.ClientRepository;
import com.undangan.online.repository.UsersRepository;
import com.undangan.online.security.JwtTokenProvider;
import com.undangan.online.service.AuditLogService;
import com.undangan.online.service.AuthService;
import com.undangan.online.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UsersRepository usersRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordPolicyConfig passwordPolicyConfig;

    public AuthServiceImpl(UsersRepository usersRepository, ClientRepository clientRepository,
                           PasswordEncoder passwordEncoder, JwtConfig jwtConfig, JwtTokenProvider tokenProvider,
                           AuditLogService auditLogService, RefreshTokenService refreshTokenService,
                           PasswordPolicyConfig passwordPolicyConfig) {
        this.usersRepository = usersRepository;
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtConfig = jwtConfig;
        this.tokenProvider = tokenProvider;
        this.auditLogService = auditLogService;
        this.refreshTokenService = refreshTokenService;
        this.passwordPolicyConfig = passwordPolicyConfig;
    }

    @Override
    public LoginResponse login(String username, String password, String ipAddress, String userAgent) {
        Optional<Users> userOpt = usersRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("Login failed: username '{}' not found", username);
            auditLogService.logAction(null, username, null, "LOGIN_FAILED", "auth", null, null, null, ipAddress, userAgent);
            throw new AuthException("INVALID_CREDENTIALS", "Username atau password salah", 401);
        }

        Users user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed: invalid password for username '{}'", username);
            auditLogService.logAction(user.getId(), user.getUsername(), user.getRoleCode(), "LOGIN_FAILED", "auth", user.getId(), null, null, ipAddress, userAgent);
            throw new AuthException("INVALID_CREDENTIALS", "Username atau password salah", 401);
        }

        if (!"active".equals(user.getStatus())) {
            log.warn("Login blocked: user '{}' is inactive", username);
            auditLogService.logAction(user.getId(), user.getUsername(), user.getRoleCode(), "LOGIN_FAILED", "auth", user.getId(), null, null, ipAddress, userAgent);
            throw new AuthException("USER_INACTIVE", "Akun tidak aktif", 403);
        }

        Long clientId = user.getClientId();
        if (clientId != null) {
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new AuthException("CLIENT_EXPIRED", "Akun sudah expired", 403));
            if (client.getExpiresAt().isBefore(LocalDate.now())) {
                log.warn("Login blocked: client {} expired for user '{}'", clientId, username);
                auditLogService.logAction(user.getId(), user.getUsername(), user.getRoleCode(), "LOGIN_FAILED", "auth", user.getId(), null, null, ipAddress, userAgent);
                throw new AuthException("CLIENT_EXPIRED", "Akun sudah expired", 403);
            }
        }

        String role = "ROLE_" + user.getRoleCode();
        String token = tokenProvider.generateToken(user.getUsername(), role, clientId);

        UserDto userDto = new UserDto(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getRoleCode(),
                user.getClientId()
        );

        String refreshToken = refreshTokenService.generateRefreshToken(user.getId(), userAgent, ipAddress);
        auditLogService.logAction(user.getId(), user.getUsername(), user.getRoleCode(), "LOGIN", "auth", user.getId(), null, null, ipAddress, userAgent);
        log.info("User {} logged in successfully (role={})", username, user.getRoleCode());
        return new LoginResponse(token, "Bearer", jwtConfig.getExpirationMs() / 1000, userDto, refreshToken, jwtConfig.getRefreshExpirationMs() / 1000);
    }

    @Override
    public String generateAccessToken(Users user) {
        String role = "ROLE_" + user.getRoleCode();
        return tokenProvider.generateToken(user.getUsername(), role, user.getClientId());
    }

    @Override
    public long getAccessTokenExpiresIn() {
        return jwtConfig.getExpirationMs() / 1000;
    }

    @Override
    public long getRefreshTokenExpiresIn() {
        return jwtConfig.getRefreshExpirationMs() / 1000;
    }

    @Override
    public UserDto registerClient(RegisterClientRequest request) {
        if (usersRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("Register failed: username '{}' already exists", request.getUsername());
            throw new AuthException("CONFLICT", "Username sudah digunakan", 409);
        }
        if (usersRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Register failed: email '{}' already exists", request.getEmail());
            throw new AuthException("CONFLICT", "Email sudah digunakan", 409);
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new AuthException("VALIDATION_ERROR", "Password minimal 8 karakter", 400);
        }
        passwordPolicyConfig.validate(request.getPassword());
        if (request.getClientId() == null) {
            throw new AuthException("VALIDATION_ERROR", "clientId wajib diisi", 400);
        }

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new AuthException("NOT_FOUND", "Client tidak ditemukan", 404));
        if (!"active".equals(client.getStatus())) {
            throw new AuthException("CLIENT_EXPIRED", "Client tidak aktif", 403);
        }
        if (client.getExpiresAt().isBefore(LocalDate.now())) {
            throw new AuthException("CLIENT_EXPIRED", "Akun sudah expired", 403);
        }

        Users user = new Users();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRoleCode("USER");
        user.setClientId(request.getClientId());
        user.setStatus("active");
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());

        usersRepository.save(user);

        log.info("User {} registered as USER for clientId={} by admin action",
                user.getUsername(), user.getClientId());
        return new UserDto(user.getId(), user.getUsername(), user.getName(), user.getRoleCode(), user.getClientId());
    }
}
