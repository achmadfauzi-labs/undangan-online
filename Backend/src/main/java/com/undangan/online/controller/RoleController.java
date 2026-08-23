package com.undangan.online.controller;

import com.undangan.online.dto.ApiResponse;
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

@RestController
@RequestMapping("/api/v1/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDto>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.listAll()));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<RoleDto>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getByCode(code)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleDto>> create(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(roleService.create(request)));
    }

    @PutMapping("/{code}")
    public ResponseEntity<ApiResponse<RoleDto>> update(@PathVariable String code,
                                                      @Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Data berhasil diupdate", roleService.update(code, request)));
    }

    @PutMapping("/{code}/permissions")
    public ResponseEntity<ApiResponse<RoleDto>> updatePermissions(@PathVariable String code,
                                                                    @Valid @RequestBody UpdateRolePermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Permission berhasil diupdate", roleService.updatePermissions(code, request)));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String code) {
        roleService.delete(code);
        return ResponseEntity.ok(ApiResponse.ok("Data berhasil dihapus", null));
    }

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<String>>> listPermissionOptions() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.listPermissionOptions()));
    }

    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<Void>> seed() {
        roleService.seedDefaultRoles();
        return ResponseEntity.ok(ApiResponse.ok("Default roles seeded", null));
    }
}
