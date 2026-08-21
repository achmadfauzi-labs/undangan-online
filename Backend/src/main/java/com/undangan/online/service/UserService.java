package com.undangan.online.service;

import com.undangan.online.dto.CreateUserRequest;
import com.undangan.online.dto.UpdateUserRequest;
import com.undangan.online.dto.UserListDto;
import com.undangan.online.entity.Role;
import com.undangan.online.entity.Users;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.RoleRepository;
import com.undangan.online.repository.UsersRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Service
public class UserService {

    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public UserService(UsersRepository usersRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UserListDto> list(Long clientId, String roleCode, String status, String search, Pageable pageable) {
        List<Users> all = usersRepository.findAll();

        List<Users> filtered = all.stream()
                .filter(u -> clientId == null || (u.getClientId() != null && u.getClientId().equals(clientId)))
                .filter(u -> roleCode == null || roleCode.isBlank() || roleCode.equals(u.getRoleCode()))
                .filter(u -> status == null || status.isBlank() || status.equals(u.getStatus()))
                .filter(u -> search == null || search.isBlank() ||
                        (u.getName() != null && u.getName().toLowerCase().contains(search.toLowerCase())) ||
                        (u.getUsername() != null && u.getUsername().toLowerCase().contains(search.toLowerCase())))
                .sorted(Comparator.comparing(Users::getCreatedAt).reversed())
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<UserListDto> content = new ArrayList<>();
        for (int i = start; i < end; i++) {
            content.add(toListDto(filtered.get(i)));
        }

        return new PageImpl<>(content, pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public UserListDto getById(Long id) {
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User tidak ditemukan"));
        return toListDto(user);
    }

    @Transactional
    public UserListDto create(CreateUserRequest request) {
        if (usersRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AuthException("CONFLICT", "Username sudah digunakan", 409);
        }
        if (usersRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AuthException("CONFLICT", "Email sudah digunakan", 409);
        }

        Role role = roleRepository.findById(request.getRoleCode())
                .orElseThrow(() -> new AuthException("NOT_FOUND", "Role tidak ditemukan", 404));

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new AuthException("VALIDATION_ERROR", "Password minimal 8 karakter", 400);
        }

        Users user = new Users();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setRoleCode(request.getRoleCode());
        user.setClientId(request.getClientId());
        user.setStatus("active");
        user.setAvatarColor(generateRandomColor());
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());

        usersRepository.save(user);

        return toListDto(user);
    }

    @Transactional
    public UserListDto update(Long id, UpdateUserRequest request) {
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User tidak ditemukan"));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (usersRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new AuthException("CONFLICT", "Email sudah digunakan", 409);
            }
            user.setEmail(request.getEmail());
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getRoleCode() != null) {
            if (!roleRepository.existsById(request.getRoleCode())) {
                throw new AuthException("NOT_FOUND", "Role tidak ditemukan", 404);
            }
            user.setRoleCode(request.getRoleCode());
        }

        user.setUpdatedAt(OffsetDateTime.now());
        usersRepository.save(user);

        return toListDto(user);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new AuthException("VALIDATION_ERROR", "Password minimal 8 karakter", 400);
        }

        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User tidak ditemukan"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(OffsetDateTime.now());
        usersRepository.save(user);
    }

    @Transactional
    public UserListDto updateStatus(Long id, String status) {
        if (!"active".equals(status) && !"inactive".equals(status)) {
            throw new AuthException("VALIDATION_ERROR", "Status harus 'active' atau 'inactive'", 400);
        }

        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User tidak ditemukan"));

        user.setStatus(status);
        user.setUpdatedAt(OffsetDateTime.now());
        usersRepository.save(user);

        return toListDto(user);
    }

    @Transactional
    public void delete(Long id) {
        if (!usersRepository.existsById(id)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "User tidak ditemukan");
        }
        usersRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<UserListDto> listByClient(Long clientId, Pageable pageable) {
        return list(clientId, null, null, null, pageable);
    }

    private UserListDto toListDto(Users user) {
        return new UserListDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getAvatarColor(),
                user.getRoleCode(),
                user.getClientId(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    private String generateRandomColor() {
        Random random = new Random();
        String hex = String.format("#%06x", random.nextInt(0xFFFFFF));
        return hex;
    }
}
