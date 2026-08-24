package com.undangan.online.service.impl;

import com.undangan.online.dto.CreateSystemParameterRequest;
import com.undangan.online.dto.SystemParameterDto;
import com.undangan.online.entity.SystemParameter;
import com.undangan.online.entity.Users;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.SystemParameterRepository;
import com.undangan.online.service.SystemParameterService;
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
import java.util.Map;

@Service
public class SystemParameterServiceImpl implements SystemParameterService {

    private static final Logger log = LoggerFactory.getLogger(SystemParameterServiceImpl.class);

    private final SystemParameterRepository systemParameterRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public SystemParameterServiceImpl(SystemParameterRepository systemParameterRepository) {
        this.systemParameterRepository = systemParameterRepository;
    }

    // ===================== READ =====================

    @Override
    @Transactional(readOnly = true)
    public List<SystemParameterDto> listAll(String groupCode, String status, String search) {
        List<SystemParameter> params = systemParameterRepository.findAll();
        return params.stream()
                .filter(p -> groupCode == null || groupCode.isBlank() || groupCode.equals(p.getGroupCode()))
                .filter(p -> status == null || status.isBlank() || status.equals(p.getStatus()))
                .filter(p -> search == null || search.isBlank() ||
                        (p.getName() != null && p.getName().toLowerCase().contains(search.toLowerCase())) ||
                        (p.getCode() != null && p.getCode().toLowerCase().contains(search.toLowerCase())))
                .sorted(Comparator.comparing(SystemParameter::getGroupCode).thenComparing(SystemParameter::getSortOrder))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SystemParameterDto getById(Long id) {
        SystemParameter param = systemParameterRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "System parameter tidak ditemukan"));
        return toDto(param);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemParameterDto> getByGroup(String groupCode) {
        TypedQuery<SystemParameter> query = entityManager.createQuery(
                "SELECT p FROM SystemParameter p WHERE p.groupCode = :groupCode", SystemParameter.class);
        query.setParameter("groupCode", groupCode);
        List<SystemParameter> params = query.getResultList();

        return params.stream()
                .sorted(Comparator.comparing(SystemParameter::getSortOrder))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SystemParameterDto getValue(String groupCode, String code) {
        SystemParameter param = systemParameterRepository.findByGroupCodeAndCode(groupCode, code)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Parameter tidak ditemukan"));
        return toDto(param);
    }

    // ===================== WRITE =====================

    @Override
    @Transactional
    public SystemParameterDto create(CreateSystemParameterRequest request) {
        String ucGroupCode = request.getGroupCode().toUpperCase();
        String code = request.getCode();
        if (systemParameterRepository.findByGroupCodeAndCode(ucGroupCode, code).isPresent()) {
            log.warn("System parameter create failed: groupCode={}, code={} already exists, by admin {}",
                    ucGroupCode, code, getCurrentUsername());
            throw new AuthException("CONFLICT", "Parameter dengan groupCode + code sudah ada", 409);
        }

        SystemParameter param = new SystemParameter();
        param.setGroupCode(ucGroupCode);
        param.setCode(code);
        param.setName(request.getName());
        param.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        param.setStatus(request.getStatus() != null ? request.getStatus() : "active");
        param.setCreatedAt(OffsetDateTime.now());
        param.setUpdatedAt(OffsetDateTime.now());

        systemParameterRepository.save(param);
        log.info("System parameter created: groupCode={}, code={}, by admin {}",
                ucGroupCode, code, getCurrentUsername());
        return toDto(param);
    }

    @Override
    @Transactional
    public SystemParameterDto update(Long id, CreateSystemParameterRequest request) {
        SystemParameter param = systemParameterRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "System parameter tidak ditemukan"));

        if (request.getName() != null) {
            param.setName(request.getName());
        }
        if (request.getSortOrder() != null) {
            param.setSortOrder(request.getSortOrder());
        }

        param.setUpdatedAt(OffsetDateTime.now());
        systemParameterRepository.save(param);
        log.info("System parameter {} updated: groupCode={}, code={}, by admin {}",
                id, param.getGroupCode(), param.getCode(), getCurrentUsername());
        return toDto(param);
    }

    @Override
    @Transactional
    public SystemParameterDto updateStatus(Long id, String status) {
        if (!"active".equals(status) && !"inactive".equals(status)) {
            throw new AuthException("VALIDATION_ERROR", "Status harus 'active' atau 'inactive'", 400);
        }

        SystemParameter param = systemParameterRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "System parameter tidak ditemukan"));

        param.setStatus(status);
        param.setUpdatedAt(OffsetDateTime.now());
        systemParameterRepository.save(param);
        log.info("System parameter {} status changed to {}: groupCode={}, code={}, by admin {}",
                id, status, param.getGroupCode(), param.getCode(), getCurrentUsername());
        return toDto(param);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SystemParameter param = systemParameterRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "System parameter tidak ditemukan"));

        String groupCode = param.getGroupCode();
        String code = param.getCode();
        Long paramId = param.getId();
        systemParameterRepository.delete(param);
        log.warn("System parameter {} deleted: groupCode={}, code={}, by admin {}",
                paramId, groupCode, code, getCurrentUsername());
    }

    @Override
    @Transactional
    public Map<String, Object> batchUpdateStatus(List<Map<String, Object>> updates) {
        int updatedCount = 0;
        for (Map<String, Object> update : updates) {
            Long id = ((Number) update.get("id")).longValue();
            String status = (String) update.get("status");

            if (!"active".equals(status) && !"inactive".equals(status)) {
                continue;
            }

            SystemParameter param = systemParameterRepository.findById(id).orElse(null);
            if (param != null) {
                param.setStatus(status);
                param.setUpdatedAt(OffsetDateTime.now());
                systemParameterRepository.save(param);
                updatedCount++;
            }
        }

        return Map.of("updated", updatedCount);
    }

    @Override
    @Transactional
    public Map<String, Object> seedDefaultParameters() {
        int seeded = 0;
        int skipped = 0;

        if (seedIfNotExists("client_package", "default_duration_months", "12", (short) 1)) seeded++; else skipped++;
        if (seedIfNotExists("client_package", "expiry_buffer_days", "7", (short) 2)) seeded++; else skipped++;
        if (seedIfNotExists("system", "invitation_base_url", "https://undangan.domain.com", (short) 1)) seeded++; else skipped++;
        if (seedIfNotExists("system", "max_guest_bulk", "100", (short) 2)) seeded++; else skipped++;
        if (seedIfNotExists("file_upload", "max_image_size_mb", "5", (short) 1)) seeded++; else skipped++;
        if (seedIfNotExists("file_upload", "max_music_size_mb", "10", (short) 2)) seeded++; else skipped++;
        if (seedIfNotExists("file_upload", "allowed_image_extensions", "jpg,jpeg,png,webp", (short) 3)) seeded++; else skipped++;
        if (seedIfNotExists("file_upload", "allowed_music_extensions", "mp3,wav,ogg", (short) 4)) seeded++; else skipped++;
        if (seedIfNotExists("event_type", "pernikahan", "Pernikahan", (short) 1)) seeded++; else skipped++;
        if (seedIfNotExists("event_type", "mutrasi", "Mutrasi", (short) 2)) seeded++; else skipped++;
        if (seedIfNotExists("event_type", "ulang_tahun", "Ulang Tahun", (short) 3)) seeded++; else skipped++;
        if (seedIfNotExists("event_type", "khitanan", "Khitanan", (short) 4)) seeded++; else skipped++;
        if (seedIfNotExists("guest_category", "family", "Keluarga", (short) 1)) seeded++; else skipped++;
        if (seedIfNotExists("guest_category", "friend", "Teman", (short) 2)) seeded++; else skipped++;
        if (seedIfNotExists("guest_category", "coworker", "Rekan Kerja", (short) 3)) seeded++; else skipped++;
        if (seedIfNotExists("guest_category", "neighbor", "Tetangga", (short) 4)) seeded++; else skipped++;
        if (seedIfNotExists("person_role", "pria", "Pria", (short) 1)) seeded++; else skipped++;
        if (seedIfNotExists("person_role", "wanita", "Wanita", (short) 2)) seeded++; else skipped++;

        log.info("System parameters seeded: {} seeded, {} skipped, by admin {}", seeded, skipped, getCurrentUsername());
        return Map.of("seeded", seeded, "skipped", skipped);
    }

    private boolean seedIfNotExists(String groupCode, String code, String name, short sortOrder) {
        String ucGroupCode = groupCode.toUpperCase();
        if (systemParameterRepository.findByGroupCodeAndCode(ucGroupCode, code).isPresent()) {
            return false;
        }
        SystemParameter param = new SystemParameter();
        param.setGroupCode(ucGroupCode);
        param.setCode(code);
        param.setName(name);
        param.setSortOrder((short) sortOrder);
        param.setStatus("active");
        param.setCreatedAt(OffsetDateTime.now());
        param.setUpdatedAt(OffsetDateTime.now());
        systemParameterRepository.save(param);
        return true;
    }

    private SystemParameterDto toDto(SystemParameter param) {
        SystemParameterDto dto = new SystemParameterDto();
        dto.setId(param.getId());
        dto.setGroupCode(param.getGroupCode());
        dto.setCode(param.getCode());
        dto.setName(param.getName());
        dto.setSortOrder(param.getSortOrder());
        dto.setStatus(param.getStatus());
        dto.setCreatedAt(param.getCreatedAt());
        dto.setUpdatedAt(param.getUpdatedAt());
        return dto;
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Users user) {
            return user.getUsername();
        }
        return "unknown";
    }
}
