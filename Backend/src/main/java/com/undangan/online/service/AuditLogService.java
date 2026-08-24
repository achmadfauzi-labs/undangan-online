package com.undangan.online.service;

import com.undangan.online.dto.AuditLogDto;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    void logAction(Long userId, String username, String role, String action, String module, Long entityId, String oldValue, String newValue, String ipAddress, String userAgent);

    com.undangan.online.dto.PageResponse<AuditLogDto> listAll(Long userId, String module, String action, java.time.OffsetDateTime startDate, java.time.OffsetDateTime endDate, int page, int size);

    AuditLogDto getDetail(Long id);
}
