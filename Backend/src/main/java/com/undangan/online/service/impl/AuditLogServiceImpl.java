package com.undangan.online.service.impl;

import com.undangan.online.dto.AuditLogDto;
import com.undangan.online.entity.AuditLog;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.AuditLogRepository;
import com.undangan.online.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void logAction(Long userId, String username, String role, String action, String module, Long entityId, String oldValue, String newValue, String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setUsername(username);
            auditLog.setRole(role);
            auditLog.setAction(action);
            auditLog.setModule(module);
            auditLog.setEntityId(entityId);
            auditLog.setOldValue(oldValue);
            auditLog.setNewValue(newValue);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setCreatedAt(OffsetDateTime.now());

            auditLogRepository.save(auditLog);
            log.info("Audit log recorded: action={}, module={}, userId={}, entityId={}", action, module, userId, entityId);
        } catch (Exception e) {
            log.warn("Gagal mencatat audit log: {}", e.getMessage());
        }
    }

    @Override
    public com.undangan.online.dto.PageResponse<AuditLogDto> listAll(Long userId, String module, String action, OffsetDateTime startDate, OffsetDateTime endDate, int page, int size) {
        int pageSize = size > 0 ? size : DEFAULT_PAGE_SIZE;
        Pageable pageable = PageRequest.of(page, pageSize);
        Specification<AuditLog> spec = (root, query, cb) -> cb.conjunction();

        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (module != null && !module.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("module"), module));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        }
        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }

        Page<AuditLog> auditLogPage = auditLogRepository.findAll(spec, pageable);

        return new com.undangan.online.dto.PageResponse<>(
                auditLogPage.getContent().stream().map(AuditLogServiceImpl::toDto).toList(),
                auditLogPage.getTotalElements(),
                auditLogPage.getTotalPages(),
                auditLogPage.getNumber(),
                auditLogPage.getSize(),
                auditLogPage.isEmpty(),
                auditLogPage.isFirst(),
                auditLogPage.isLast()
        );
    }

    @Override
    public AuditLogDto getDetail(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new AuthException("NOT_FOUND", "Audit log tidak ditemukan", 404));
        return toDto(auditLog);
    }

    private static AuditLogDto toDto(AuditLog auditLog) {
        return new AuditLogDto(
                auditLog.getId(),
                auditLog.getUserId(),
                auditLog.getUsername(),
                auditLog.getRole(),
                auditLog.getAction(),
                auditLog.getModule(),
                auditLog.getEntityId(),
                auditLog.getOldValue(),
                auditLog.getNewValue(),
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getCreatedAt()
        );
    }
}
