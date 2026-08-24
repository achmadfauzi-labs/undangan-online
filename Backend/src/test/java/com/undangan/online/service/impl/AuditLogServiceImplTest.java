package com.undangan.online.service.impl;

import com.undangan.online.dto.AuditLogDto;
import com.undangan.online.entity.AuditLog;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    void logAction_success() {
        auditLogService.logAction(1L, "admin", "ADMIN", "CREATE", "user", 10L, null, null, "127.0.0.1", "Mozilla");
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void logAction_doesNotThrowOnException() {
        doThrow(new RuntimeException("DB error")).when(auditLogRepository).save(any());
        assertDoesNotThrow(() -> auditLogService.logAction(1L, "admin", "ADMIN", "CREATE", "user", 10L, null, null, "127.0.0.1", "Mozilla"));
    }

    @Test
    void getDetail_found() {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(1L);
        auditLog.setAction("LOGIN");
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(auditLog));

        AuditLogDto dto = auditLogService.getDetail(1L);
        assertEquals(1L, dto.getId());
        assertEquals("LOGIN", dto.getAction());
    }

    @Test
    void getDetail_notFound() {
        when(auditLogRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(AuthException.class, () -> auditLogService.getDetail(999L));
    }

    @Test
    void listAll_returnsPagedResult() {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(1L);
        auditLog.setAction("LOGIN");
        Page<AuditLog> page = new PageImpl<>(List.of(auditLog), Pageable.ofSize(20), 1);

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        var result = auditLogService.listAll(1L, "auth", "LOGIN", null, null, 0, 20);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
    }
}
