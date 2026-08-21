package com.undangan.online.controller;

import com.undangan.online.dto.CreateRoleRequest;
import com.undangan.online.dto.RoleDto;
import com.undangan.online.dto.UpdateRolePermissionsRequest;
import com.undangan.online.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<List<RoleDto>> list() {
        return ResponseEntity.ok(roleService.listAll());
    }

    @GetMapping("/{code}")
    public ResponseEntity<RoleDto> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(roleService.getByCode(code));
    }

    @PostMapping
    public ResponseEntity<RoleDto> create(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(request));
    }

    @PutMapping("/{code}")
    public ResponseEntity<RoleDto> update(@PathVariable String code,
                                          @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(roleService.update(code, request));
    }

    @PutMapping("/{code}/permissions")
    public ResponseEntity<RoleDto> updatePermissions(@PathVariable String code,
                                                     @Valid @RequestBody UpdateRolePermissionsRequest request) {
        return ResponseEntity.ok(roleService.updatePermissions(code, request));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        roleService.delete(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<String>> listPermissionOptions() {
        return ResponseEntity.ok(roleService.listPermissionOptions());
    }

    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seed() {
        roleService.seedDefaultRoles();
        return ResponseEntity.ok(Map.of("message", "Default roles seeded"));
    }
}
