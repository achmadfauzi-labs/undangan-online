package com.undangan.online.service;

import com.undangan.online.config.JwtConfig;
import com.undangan.online.dto.LoginResponse;
import com.undangan.online.dto.RegisterClientRequest;
import com.undangan.online.dto.UserDto;
import com.undangan.online.entity.Client;
import com.undangan.online.entity.Users;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.ClientRepository;
import com.undangan.online.repository.UsersRepository;
import com.undangan.online.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class AuthService {

    private final UsersRepository usersRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UsersRepository usersRepository, ClientRepository clientRepository,
                       PasswordEncoder passwordEncoder, JwtConfig jwtConfig, JwtTokenProvider tokenProvider) {
        this.usersRepository = usersRepository;
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtConfig = jwtConfig;
        this.tokenProvider = tokenProvider;
    }

    public LoginResponse login(String username, String password) {
        Optional<Users> userOpt = usersRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new AuthException("INVALID_CREDENTIALS", "Username atau password salah", 401);
        }

        Users user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthException("INVALID_CREDENTIALS", "Username atau password salah", 401);
        }

        if (!"active".equals(user.getStatus())) {
            throw new AuthException("USER_INACTIVE", "Akun tidak aktif", 403);
        }

        if (user.getClientId() != null) {
            Client client = clientRepository.findById(user.getClientId())
                    .orElseThrow(() -> new AuthException("CLIENT_EXPIRED", "Akun sudah expired", 403));
            if (client.getExpiresAt().isBefore(LocalDate.now())) {
                throw new AuthException("CLIENT_EXPIRED", "Akun sudah expired", 403);
            }
        }

        String role = "ROLE_" + user.getRoleCode();
        String token = tokenProvider.generateToken(user.getUsername(), role, user.getClientId());

        UserDto userDto = new UserDto(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getRoleCode(),
                user.getClientId()
        );

        return new LoginResponse(token, "Bearer", jwtConfig.getExpirationMs() / 1000, userDto);
    }

    public UserDto registerClient(RegisterClientRequest request) {
        if (usersRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AuthException("CONFLICT", "Username sudah digunakan", 409);
        }
        if (usersRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AuthException("CONFLICT", "Email sudah digunakan", 409);
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new AuthException("VALIDATION_ERROR", "Password minimal 8 karakter", 400);
        }
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
        user.setCreatedAt(java.time.OffsetDateTime.now());
        user.setUpdatedAt(java.time.OffsetDateTime.now());

        usersRepository.save(user);

        return new UserDto(user.getId(), user.getUsername(), user.getName(), user.getRoleCode(), user.getClientId());
    }
}
