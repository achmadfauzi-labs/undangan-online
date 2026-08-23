package com.undangan.online.service.impl;

import com.undangan.online.dto.CreateThemeRequest;
import com.undangan.online.dto.ThemeDto;
import com.undangan.online.dto.ThemeListDto;
import com.undangan.online.dto.UpdateThemeRequest;
import com.undangan.online.entity.Invitation;
import com.undangan.online.entity.Template;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.InvitationRepository;
import com.undangan.online.repository.TemplateRepository;
import com.undangan.online.service.ThemeManagementService;
import com.undangan.online.service.ThemeStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ThemeManagementServiceImpl implements ThemeManagementService {

    private static final Logger log = LoggerFactory.getLogger(ThemeManagementServiceImpl.class);

    private final TemplateRepository templateRepository;
    private final InvitationRepository invitationRepository;
    private final ThemeStorageService themeStorageService;

    public ThemeManagementServiceImpl(TemplateRepository templateRepository,
                                      InvitationRepository invitationRepository,
                                      ThemeStorageService themeStorageService) {
        this.templateRepository = templateRepository;
        this.invitationRepository = invitationRepository;
        this.themeStorageService = themeStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThemeListDto> listAll(String status, String category, String search) {
        List<Template> themes = templateRepository.findAll();
        return themes.stream()
                .filter(t -> status == null || status.isBlank() || status.equals(t.getStatus()))
                .filter(t -> category == null || category.isBlank() || category.equals(t.getCategory()))
                .filter(t -> search == null || search.isBlank() ||
                        (t.getName() != null && t.getName().toLowerCase().contains(search.toLowerCase())) ||
                        (t.getCode() != null && t.getCode().toLowerCase().contains(search.toLowerCase())))
                .sorted(Comparator.comparing(Template::getCategory).thenComparing(Template::getName))
                .map(this::toListDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ThemeDto getById(Long id) {
        Template theme = templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Theme tidak ditemukan"));
        return toDto(theme);
    }

    @Override
    @Transactional
    public ThemeDto create(CreateThemeRequest request, MultipartFile zipFile) throws IOException {
        if (templateRepository.findByCode(request.getCode()).isPresent()) {
            log.error("Theme create failed: code='{}' sudah digunakan, by admin {}", request.getCode(), getCurrentUsername());
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
            theme.setFolderPath("/");
        }

        templateRepository.save(theme);
        log.info("Theme {} created by admin {}", request.getCode(), getCurrentUsername());
        return toDto(theme);
    }

    @Override
    @Transactional
    public ThemeDto update(Long id, UpdateThemeRequest request, MultipartFile zipFile) throws IOException {
        Template theme = templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Theme tidak ditemukan"));

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
        log.info("Theme {} updated by admin {}", id, getCurrentUsername());
        return toDto(theme);
    }

    @Override
    @Transactional
    public ThemeDto updateStatus(Long id, String status) {
        if (!"active".equals(status) && !"inactive".equals(status)) {
            log.error("Theme status update failed: id={}, invalid status='{}', by admin {}", id, status, getCurrentUsername());
            throw new AuthException("VALIDATION_ERROR", "Status harus 'active' atau 'inactive'", 400);
        }

        Template theme = templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Theme tidak ditemukan"));

        String oldStatus = theme.getStatus();
        theme.setStatus(status);
        theme.setUpdatedAt(OffsetDateTime.now());
        templateRepository.save(theme);
        log.info("Theme {} status changed to {} by admin {}", id, status, getCurrentUsername());
        if (oldStatus != null && !oldStatus.equals(status)) {
            if ("active".equals(status)) {
                log.info("Theme {} activated by admin {}", id, getCurrentUsername());
            } else {
                log.info("Theme {} deactivated by admin {}", id, getCurrentUsername());
            }
        }
        return toDto(theme);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Template theme = templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Theme tidak ditemukan"));
        try {
            templateRepository.delete(theme);
            if (theme.getFolderPath() != null && !theme.getFolderPath().isBlank()) {
                themeStorageService.deleteThemeFolder(theme.getCode());
            }
            log.warn("Theme {} deleted by admin {}", id, getCurrentUsername());
        } catch (DataIntegrityViolationException ex) {
            log.error("Theme delete failed: id={}, by admin {}", id, getCurrentUsername(), ex);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Theme masih dipakai, tidak bisa dihapus");
        } catch (IOException ex) {
            log.error("Theme folder delete failed: id={}, code='{}', by admin {}", id, theme.getCode(), getCurrentUsername(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal menghapus file tema");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listCategories() {
        return templateRepository.findAll().stream()
                .map(Template::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ThemeDto uploadZip(Long id, MultipartFile zipFile) throws IOException {
        Template theme = templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Theme tidak ditemukan"));

        if (zipFile == null || zipFile.isEmpty()) {
            log.error("Theme upload failed: id={}, reason=file kosong, by admin {}", id, getCurrentUsername());
            throw new AuthException("VALIDATION_ERROR", "File ZIP wajib diupload", 400);
        }

        try {
            String folderPath = themeStorageService.extractZipToThemeFolder(theme.getCode(), zipFile);
            theme.setFolderPath(folderPath);
            theme.setUpdatedAt(OffsetDateTime.now());
            templateRepository.save(theme);
            log.info("Theme {} zip uploaded by admin {}", id, getCurrentUsername());
            return toDto(theme);
        } catch (IOException ex) {
            log.error("Theme {} zip upload failed, by admin {}", id, getCurrentUsername(), ex);
            throw ex;
        }
    }

    // Client methods

    @Override
    @Transactional(readOnly = true)
    public List<ThemeListDto> listActive() {
        List<Template> themes = templateRepository.findByStatus("active");
        return themes.stream()
                .sorted(Comparator.comparing(Template::getCategory).thenComparing(Template::getName))
                .map(this::toListDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ThemeDto getActiveById(Long id) {
        Template theme = templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Theme tidak ditemukan"));
        if (!"active".equals(theme.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Theme tidak aktif");
        }
        return toDto(theme);
    }

    @Override
    @Transactional
    public void assignToMyInvitation(Long clientId, Long themeId) {
        Invitation invitation = invitationRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation belum dibuat"));

        Template theme = templateRepository.findById(themeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Theme tidak ditemukan"));

        if (!"active".equals(theme.getStatus())) {
            throw new AuthException("VALIDATION_ERROR", "Theme tidak aktif", 400);
        }

        invitation.setTemplateId(themeId);
        invitation.setUpdatedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);
        log.info("Theme {} assigned to invitation of client {} by admin {}", themeId, clientId, getCurrentUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public ThemeDto getCurrentTheme(Long clientId) {
        Invitation invitation = invitationRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation belum dibuat"));

        if (invitation.getTemplateId() == null) {
            return null;
        }

        return getById(invitation.getTemplateId());
    }

    @Override
    @Transactional
    public void removeThemeFromInvitation(Long clientId) {
        Invitation invitation = invitationRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation belum dibuat"));

        invitation.setTemplateId(null);
        invitation.setUpdatedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);
        log.info("Theme removed from invitation of client {} by admin {}", clientId, getCurrentUsername());
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

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails ud) {
                return ud.getUsername();
            }
            if (principal instanceof String s && !s.isBlank()) {
                return s;
            }
            return auth.getName();
        }
        return "unknown";
    }
}
