package com.undangan.online.service.impl;

import com.undangan.online.dto.CreateRoleRequest;
import com.undangan.online.dto.RoleDto;
import com.undangan.online.dto.UpdateRolePermissionsRequest;
import com.undangan.online.entity.Role;
import com.undangan.online.entity.RolePermission;
import com.undangan.online.entity.RolePermissionId;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.RolePermissionRepository;
import com.undangan.online.repository.RoleRepository;
import com.undangan.online.repository.UsersRepository;
import com.undangan.online.service.RoleService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UsersRepository usersRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final List<String> VALID_MENU_KEYS = List.of(
            "client_management",
            "user_management",
            "invitation_management",
            "guest_management",
            "theme_management",
            "music_management",
            "gallery_management",
            "system_parameter"
    );

    public RoleServiceImpl(RoleRepository roleRepository,
                           RolePermissionRepository rolePermissionRepository,
                           UsersRepository usersRepository) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.usersRepository = usersRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<RoleDto> listAll() {
        List<Role> roles = roleRepository.findAll();
        return roles.stream()
                .sorted(Comparator.comparing(Role::getCode))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public RoleDto getByCode(String code) {
        Role role = roleRepository.findById(code)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Role tidak ditemukan"));
        return toDto(role);
    }

    @Transactional
    @Override
    public RoleDto create(CreateRoleRequest request) {
        String actorUsername = currentActor();
        if (roleRepository.findById(request.getCode()).isPresent()) {
            log.warn("Create role failed: code '{}' already exists by admin {}", request.getCode(), actorUsername);
            throw new AuthException("CONFLICT", "Code role sudah digunakan", 409);
        }

        Role role = new Role();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setIsSystem(false);
        role.setCreatedAt(OffsetDateTime.now());
        role.setUpdatedAt(OffsetDateTime.now());

        List<RolePermission> permissions = new ArrayList<>();
        if (request.getPermissions() != null) {
            for (String menuKey : request.getPermissions()) {
                if (!VALID_MENU_KEYS.contains(menuKey)) {
                    throw new AuthException("VALIDATION_ERROR", "menu_key tidak valid: " + menuKey, 400);
                }
                RolePermission rp = new RolePermission();
                RolePermissionId id = new RolePermissionId();
                id.setRoleCode(request.getCode());
                id.setMenuKey(menuKey);
                rp.setId(id);
                rp.setRole(role);
                permissions.add(rp);
            }
        }
        role.setPermissions(permissions);

        roleRepository.save(role);
        log.info("Role {} created by admin {}", request.getCode(), actorUsername);
        return toDto(role);
    }

    @Transactional
    @Override
    public RoleDto update(String code, CreateRoleRequest request) {
        String actorUsername = currentActor();
        Role role = roleRepository.findById(code)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Role tidak ditemukan"));

        if (role.getIsSystem() && !code.equals(request.getCode())) {
            throw new AuthException("FORBIDDEN", "Tidak bisa mengubah code role system", 403);
        }

        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setUpdatedAt(OffsetDateTime.now());
        roleRepository.save(role);

        log.info("Role {} updated by admin {}", code, actorUsername);
        return toDto(role);
    }

    @Transactional
    @Override
    public RoleDto updatePermissions(String code, UpdateRolePermissionsRequest request) {
        String actorUsername = currentActor();
        Role role = roleRepository.findById(code)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Role tidak ditemukan"));

        List<RolePermission> managedPermissions = role.getPermissions();
        managedPermissions.clear();

        List<RolePermission> newPermissions = new ArrayList<>();
        if (request.getPermissions() != null) {
            for (String menuKey : request.getPermissions()) {
                if (!VALID_MENU_KEYS.contains(menuKey)) {
                    throw new AuthException("VALIDATION_ERROR", "menu_key tidak valid: " + menuKey, 400);
                }
                RolePermission rp = new RolePermission();
                RolePermissionId id = new RolePermissionId();
                id.setRoleCode(code);
                id.setMenuKey(menuKey);
                rp.setId(id);
                rp.setRole(role);
                newPermissions.add(rp);
            }
        }
        managedPermissions.addAll(newPermissions);

        roleRepository.save(role);
        log.info("Permissions updated for role {} by admin {}", code, actorUsername);
        return toDto(role);
    }

    @Transactional
    @Override
    public void delete(String code) {
        String actorUsername = currentActor();
        Role role = roleRepository.findById(code)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Role tidak ditemukan"));

        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new AuthException("FORBIDDEN", "Tidak bisa menghapus role system", 403);
        }

        Long userCount = countUsersByRole(code);
        if (userCount > 0) {
            throw new AuthException("CONFLICT", "Tidak bisa menghapus role yang masih digunakan oleh " + userCount + " user", 409);
        }

        rolePermissionRepository.deleteByRole_Code(code);
        roleRepository.delete(role);
        log.warn("Role {} deleted by admin {}", code, actorUsername);
    }

    @Transactional(readOnly = true)
    @Override
    public List<String> listPermissionOptions() {
        return new ArrayList<>(VALID_MENU_KEYS);
    }

    @Transactional
    @Override
    public void seedDefaultRoles() {
        log.info("Seeding default roles");
        seedRole("ADMIN", "Super Admin", "Role untuk Super Admin", true);
        seedRole("USER", "Client User", "Role untuk user client", true);
    }

    private void seedRole(String code, String name, String description, boolean isSystem) {
        if (roleRepository.findById(code).isEmpty()) {
            Role role = new Role();
            role.setCode(code);
            role.setName(name);
            role.setDescription(description);
            role.setIsSystem(isSystem);
            role.setCreatedAt(OffsetDateTime.now());
            role.setUpdatedAt(OffsetDateTime.now());
            roleRepository.save(role);
        }
    }

    private long countUsersByRole(String roleCode) {
        TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(u) FROM Users u WHERE u.roleCode = :roleCode", Long.class);
        query.setParameter("roleCode", roleCode);
        return query.getSingleResult();
    }

    private RoleDto toDto(Role role) {
        List<String> permissionKeys = role.getPermissions().stream()
                .map(rp -> rp.getId() != null ? rp.getId().getMenuKey() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new RoleDto(
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.getIsSystem(),
                permissionKeys
        );
    }

    private String currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof String s) {
                return "anonymousUser".equals(s) ? "system" : s;
            }
            if (principal instanceof com.undangan.online.entity.Users u) {
                return u.getUsername();
            }
            return auth.getName();
        }
        return "system";
    }
}
