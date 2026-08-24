package com.undangan.online.controller;

import com.undangan.online.dto.ApiResponse;
import com.undangan.online.dto.CreateSystemParameterRequest;
import com.undangan.online.dto.SystemParameterDto;
import com.undangan.online.service.SystemParameterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/system-parameters")
@PreAuthorize("hasRole('ADMIN')")
public class SystemParameterController {

    private final SystemParameterService systemParameterService;

    public SystemParameterController(SystemParameterService systemParameterService) {
        this.systemParameterService = systemParameterService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemParameterDto>>> list(
            @RequestParam(required = false) String groupCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(systemParameterService.listAll(groupCode, status, search)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemParameterDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(systemParameterService.getById(id)));
    }

    @GetMapping("/group/{groupCode}")
    public ResponseEntity<ApiResponse<List<SystemParameterDto>>> getByGroup(@PathVariable String groupCode) {
        return ResponseEntity.ok(ApiResponse.ok(systemParameterService.getByGroup(groupCode)));
    }

    @GetMapping("/{groupCode}/{code}/value")
    public ResponseEntity<ApiResponse<SystemParameterDto>> getValue(@PathVariable String groupCode, @PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(systemParameterService.getValue(groupCode, code)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemParameterDto>> create(@Valid @RequestBody CreateSystemParameterRequest request) {
        SystemParameterDto created = systemParameterService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Parameter berhasil dibuat", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemParameterDto>> update(@PathVariable Long id,
                                                     @Valid @RequestBody CreateSystemParameterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Parameter berhasil diupdate", systemParameterService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SystemParameterDto>> updateStatus(@PathVariable Long id,
                                                             @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(ApiResponse.ok("Status parameter berhasil diupdate", systemParameterService.updateStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        systemParameterService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Parameter berhasil dihapus", null));
    }

    @PatchMapping("/batch-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchUpdateStatus(@RequestBody List<Map<String, Object>> updates) {
        return ResponseEntity.ok(ApiResponse.ok(systemParameterService.batchUpdateStatus(updates)));
    }

    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> seed() {
        return ResponseEntity.ok(ApiResponse.ok("Default parameters seeded", systemParameterService.seedDefaultParameters()));
    }
}
