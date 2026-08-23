package com.undangan.online.controller;

import com.undangan.online.dto.ApiResponse;
import com.undangan.online.dto.CreateUserRequest;
import com.undangan.online.dto.PageResponse;
import com.undangan.online.dto.ResetPasswordRequest;
import com.undangan.online.dto.UpdateUserRequest;
import com.undangan.online.dto.UpdateUserStatusRequest;
import com.undangan.online.dto.UserListDto;
import com.undangan.online.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserListDto>>> list(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok("Berhasil mengambil data", userService.list(clientId, roleCode, status, search, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserListDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserListDto>> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(userService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserListDto>> update(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Data berhasil diupdate", userService.update(id, request)));
    }

    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id,
                                                           @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Password berhasil diubah", null));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserListDto>> updateStatus(@PathVariable Long id,
                                                                 @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Status user berhasil diupdate", userService.updateStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Data berhasil dihapus", null));
    }

    @GetMapping("/clients/{clientId}/users")
    public ResponseEntity<ApiResponse<PageResponse<UserListDto>>> listByClient(@PathVariable Long clientId,
                                                                              @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(userService.listByClient(clientId, pageable)));
    }
}
