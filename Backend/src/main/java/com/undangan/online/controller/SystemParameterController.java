package com.undangan.online.controller;

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
    public ResponseEntity<List<SystemParameterDto>> list(
            @RequestParam(required = false) String groupCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(systemParameterService.listAll(groupCode, status, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SystemParameterDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(systemParameterService.getById(id));
    }

    @GetMapping("/group/{groupCode}")
    public ResponseEntity<List<SystemParameterDto>> getByGroup(@PathVariable String groupCode) {
        return ResponseEntity.ok(systemParameterService.getByGroup(groupCode));
    }

    @GetMapping("/{groupCode}/{code}/value")
    public ResponseEntity<SystemParameterDto> getValue(@PathVariable String groupCode, @PathVariable String code) {
        return ResponseEntity.ok(systemParameterService.getValue(groupCode, code));
    }

    @PostMapping
    public ResponseEntity<SystemParameterDto> create(@Valid @RequestBody CreateSystemParameterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(systemParameterService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SystemParameterDto> update(@PathVariable Long id,
                                                     @Valid @RequestBody CreateSystemParameterRequest request) {
        return ResponseEntity.ok(systemParameterService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SystemParameterDto> updateStatus(@PathVariable Long id,
                                                            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(systemParameterService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        systemParameterService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/batch-status")
    public ResponseEntity<Map<String, Object>> batchUpdateStatus(@RequestBody List<Map<String, Object>> updates) {
        return ResponseEntity.ok(systemParameterService.batchUpdateStatus(updates));
    }

    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seed() {
        return ResponseEntity.ok(systemParameterService.seedDefaultParameters());
    }
}
