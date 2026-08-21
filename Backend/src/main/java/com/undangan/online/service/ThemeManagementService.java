package com.undangan.online.service;

import com.undangan.online.dto.CreateThemeRequest;
import com.undangan.online.dto.ThemeDto;
import com.undangan.online.dto.ThemeListDto;
import com.undangan.online.dto.UpdateThemeRequest;
import com.undangan.online.entity.Invitation;
import com.undangan.online.entity.Template;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.InvitationRepository;
import com.undangan.online.repository.TemplateRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ThemeManagementService {

    private final TemplateRepository templateRepository;
    private final InvitationRepository invitationRepository;
    private final ThemeStorageService themeStorageService;

    public ThemeManagementService(TemplateRepository templateRepository,
                                  InvitationRepository invitationRepository,
                                  ThemeStorageService themeStorageService) {
        this.templateRepository = templateRepository;
        this.invitationRepository = invitationRepository;
        this.themeStorageService = themeStorageService;
    }

    @Transactional(readOnly = true)
    public List<ThemeListDto> listAll(String status, String category, String search) {
        List<Template> themes = templateRepository.findAll();
        return themes.stream()
                .filter(t -> status == null || status.isBlank() || status.equals(t.getStatus()))
                .filter(t -> category == null || category.isBlank() || category.equals(t.getCategory()))
                .filter(t -> search == null || search.isBlank() ||
                        (t.getName() != null && t.getName().toLowerCase().contains(search.toLowerCase())) ||
                        (t.getCode() != null && t.getCode().toLowerCase().contains(search.toLowerCase())))
                .sorted(java.util.Comparator.comparing(Template::getCategory).thenComparing(Template::getName))
                .map(this::toListDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ThemeDto getById(Long id) {
        Template theme = templateRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Theme tidak ditemukan"));
        return toDto(theme);
    }

    @Transactional
    public ThemeDto create(CreateThemeRequest request, MultipartFile zipFile) throws IOException {
        if (templateRepository.findByCode(request.getCode()).isPresent()) {
            throw new AuthException("CONFLICT", "Code theme sudah digunakan", 409);
        }

        Template theme = new Template();
        theme.setCode(request.getCode());
        theme.setName(request.getName());
        theme.setCategory(request.getCategory());
        theme.setThumbnailCss(request.getThumbnailCss());
        theme.setStatus("active");
        theme.setCreatedAt(OffsetDateTime.now());
        theme.setUpdatedAt(OffsetDateTime.now());

        if (zipFile != null && !zipFile.isEmpty()) {
            String folderPath = themeStorageService.extractZipToThemeFolder(request.getCode(), zipFile);
            theme.setFolderPath(folderPath);
        } else {
            theme.setFolderPath(null);
        }

        templateRepository.save(theme);
        return toDto(theme);
    }

    @Transactional
    public ThemeDto update(Long id, UpdateThemeRequest request, MultipartFile zipFile) throws IOException {
        Template theme = templateRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Theme tidak ditemukan"));

        if (request.getName() != null) {
            theme.setName(request.getName());
        }
        if (request.getCategory() != null) {
            theme.setCategory(request.getCategory());
        }
        if (request.getThumbnailCss() != null) {
            theme.setThumbnailCss(request.getThumbnailCss());
        }

        if (zipFile != null && !zipFile.isEmpty()) {
            String folderPath = themeStorageService.extractZipToThemeFolder(theme.getCode(), zipFile);
            theme.setFolderPath(folderPath);
        }

        theme.setUpdatedAt(OffsetDateTime.now());
        templateRepository.save(theme);
        return toDto(theme);
    }

    @Transactional
    public ThemeDto updateStatus(Long id, String status) {
        if (!"active".equals(status) && !"inactive".equals(status)) {
            throw new AuthException("VALIDATION_ERROR", "Status harus 'active' atau 'inactive'", 400);
        }

        Template theme = templateRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Theme tidak ditemukan"));

        theme.setStatus(status);
        theme.setUpdatedAt(OffsetDateTime.now());
        templateRepository.save(theme);
        return toDto(theme);
    }

    @Transactional
    public void delete(Long id) {
        Template theme = templateRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Theme tidak ditemukan"));
        templateRepository.delete(theme);
    }

    @Transactional(readOnly = true)
    public List<String> listCategories() {
        return templateRepository.findAll().stream()
                .map(Template::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Transactional
    public ThemeDto uploadZip(Long id, MultipartFile zipFile) throws IOException {
        Template theme = templateRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Theme tidak ditemukan"));

        if (zipFile == null || zipFile.isEmpty()) {
            throw new AuthException("VALIDATION_ERROR", "File ZIP wajib diupload", 400);
        }

        String folderPath = themeStorageService.extractZipToThemeFolder(theme.getCode(), zipFile);
        theme.setFolderPath(folderPath);
        theme.setUpdatedAt(OffsetDateTime.now());
        templateRepository.save(theme);
        return toDto(theme);
    }

    // Client methods
    @Transactional(readOnly = true)
    public List<ThemeListDto> listActive() {
        List<Template> themes = templateRepository.findByStatus("active");
        return themes.stream()
                .sorted(java.util.Comparator.comparing(Template::getCategory).thenComparing(Template::getName))
                .map(this::toListDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ThemeDto getActiveById(Long id) {
        Template theme = templateRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Theme tidak ditemukan"));
        if (!"active".equals(theme.getStatus())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Theme tidak aktif");
        }
        return toDto(theme);
    }

    @Transactional
    public void assignToMyInvitation(Long clientId, Long themeId) {
        Invitation invitation = invitationRepository.findByClientId(clientId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Invitation belum dibuat"));

        Template theme = templateRepository.findById(themeId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Theme tidak ditemukan"));

        if (!"active".equals(theme.getStatus())) {
            throw new AuthException("VALIDATION_ERROR", "Theme tidak aktif", 400);
        }

        invitation.setTemplateId(themeId);
        invitation.setUpdatedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public ThemeDto getCurrentTheme(Long clientId) {
        Invitation invitation = invitationRepository.findByClientId(clientId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Invitation belum dibuat"));

        if (invitation.getTemplateId() == null) {
            return null;
        }

        return getById(invitation.getTemplateId());
    }

    @Transactional
    public void removeThemeFromInvitation(Long clientId) {
        Invitation invitation = invitationRepository.findByClientId(clientId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Invitation belum dibuat"));

        invitation.setTemplateId(null);
        invitation.setUpdatedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);
    }

    private ThemeDto toDto(Template theme) {
        ThemeDto dto = new ThemeDto();
        dto.setId(theme.getId());
        dto.setCode(theme.getCode());
        dto.setName(theme.getName());
        dto.setCategory(theme.getCategory());
        dto.setFolderPath(theme.getFolderPath());
        dto.setThumbnailCss(theme.getThumbnailCss());
        dto.setStatus(theme.getStatus());
        dto.setCreatedAt(theme.getCreatedAt());
        dto.setUpdatedAt(theme.getUpdatedAt());
        return dto;
    }

    private ThemeListDto toListDto(Template theme) {
        return new ThemeListDto(
                theme.getId(),
                theme.getCode(),
                theme.getName(),
                theme.getCategory(),
                theme.getFolderPath(),
                theme.getThumbnailCss(),
                theme.getStatus(),
                theme.getCreatedAt()
        );
    }
}
