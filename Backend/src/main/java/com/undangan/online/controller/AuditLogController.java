package com.undangan.online.controller;

import com.undangan.online.dto.ApiResponse;
import com.undangan.online.dto.AuditLogDto;
import com.undangan.online.dto.PageResponse;
import com.undangan.online.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) OffsetDateTime startDate,
            @RequestParam(required = false) OffsetDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok("Data audit log berhasil diambil",
                auditLogService.listAll(userId, module, action, startDate, endDate, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditLogDto>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Detail audit log berhasil diambil", auditLogService.getDetail(id)));
    }
}
