package com.undangan.online.service.impl;

import com.undangan.online.dto.CreateGalleryRequest;
import com.undangan.online.dto.GalleryDto;
import com.undangan.online.dto.UpdateGalleryRequest;
import com.undangan.online.entity.Gallery;
import com.undangan.online.entity.Invitation;
import com.undangan.online.entity.InvitationPerson;
import com.undangan.online.entity.Users;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.GalleryRepository;
import com.undangan.online.repository.InvitationPersonRepository;
import com.undangan.online.repository.InvitationRepository;
import com.undangan.online.service.GalleryService;
import com.undangan.online.service.ImageStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GalleryServiceImpl implements GalleryService {

    private static final Logger log = LoggerFactory.getLogger(GalleryServiceImpl.class);

    private final GalleryRepository galleryRepository;
    private final InvitationRepository invitationRepository;
    private final InvitationPersonRepository invitationPersonRepository;
    private final ImageStorageService imageStorageService;

    public GalleryServiceImpl(GalleryRepository galleryRepository,
                              InvitationRepository invitationRepository,
                              InvitationPersonRepository invitationPersonRepository,
                              ImageStorageService imageStorageService) {
        this.galleryRepository = galleryRepository;
        this.invitationRepository = invitationRepository;
        this.invitationPersonRepository = invitationPersonRepository;
        this.imageStorageService = imageStorageService;
    }

    private Long getClientId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthException("UNAUTHORIZED", "Tidak terautentikasi", 401);
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Users user) {
            if (user.getClientId() == null) {
                throw new AuthException("FORBIDDEN", "User bukan client", 403);
            }
            return user.getClientId();
        }
        throw new AuthException("UNAUTHORIZED", "Tidak terautentikasi", 401);
    }

    private Invitation getMyInvitationOrThrow() {
        Long clientId = getClientId();
        return invitationRepository.findByClientId(clientId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Invitation belum dibuat"));
    }

    private void verifyGalleryOwnership(Gallery gallery, Invitation invitation) {
        if (!invitation.getId().equals(gallery.getInvitationId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Gallery tidak ditemukan");
        }
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Users user) {
            return user.getUsername();
        }
        return "unknown";
    }

    // ===================== GALLERY CRUD =====================

    @Override
    @Transactional(readOnly = true)
    public List<GalleryDto> listGalleries() {
        Invitation invitation = getMyInvitationOrThrow();
        List<Gallery> galleries = galleryRepository.findByInvitationIdOrderBySortOrderAsc(invitation.getId());
        return galleries.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GalleryDto getGallery(Long galleryId) {
        Invitation invitation = getMyInvitationOrThrow();
        Gallery gallery = galleryRepository.findById(galleryId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Gallery tidak ditemukan"));
        verifyGalleryOwnership(gallery, invitation);
        return toDto(gallery);
    }

    @Override
    @Transactional
    public GalleryDto createGallery(CreateGalleryRequest request) throws IOException {
        Invitation invitation = getMyInvitationOrThrow();

        if (request.getImageFile() == null || request.getImageFile().isEmpty()) {
            throw new AuthException("VALIDATION_ERROR", "File gambar wajib diupload", 400);
        }

        String imagePath = imageStorageService.saveGalleryImage(
                invitation.getClientId(), invitation.getId(), request.getImageFile());

        Short sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = getNextSortOrder(invitation.getId());
        }

        Gallery gallery = new Gallery();
        gallery.setInvitationId(invitation.getId());
        gallery.setImagePath(imagePath);
        gallery.setCaption(request.getCaption());
        gallery.setSortOrder(sortOrder);
        gallery.setCreatedAt(OffsetDateTime.now());
        gallery.setUpdatedAt(OffsetDateTime.now());

        galleryRepository.save(gallery);
        log.info("Gallery entry created: invitationId={}, clientId={}, by {}", invitation.getId(), invitation.getClientId(), getCurrentUsername());
        return toDto(gallery);
    }

    @Override
    @Transactional
    public GalleryDto updateGallery(Long galleryId, UpdateGalleryRequest request) throws IOException {
        Invitation invitation = getMyInvitationOrThrow();
        Gallery gallery = galleryRepository.findById(galleryId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Gallery tidak ditemukan"));
        verifyGalleryOwnership(gallery, invitation);

        if (request.getCaption() != null) {
            gallery.setCaption(request.getCaption());
        }
        if (request.getSortOrder() != null) {
            gallery.setSortOrder(request.getSortOrder());
        }
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            String oldPath = gallery.getImagePath();
            String newPath = imageStorageService.saveGalleryImage(
                    invitation.getClientId(), invitation.getId(), request.getImageFile());
            imageStorageService.replaceFile(oldPath, newPath);
            gallery.setImagePath(newPath);
        }

        gallery.setUpdatedAt(OffsetDateTime.now());
        galleryRepository.save(gallery);
        log.info("Gallery entry {} updated: invitationId={}, clientId={}, by {}",
                galleryId, invitation.getId(), invitation.getClientId(), getCurrentUsername());
        return toDto(gallery);
    }

    @Override
    @Transactional
    public void deleteGallery(Long galleryId) throws IOException {
        Invitation invitation = getMyInvitationOrThrow();
        Gallery gallery = galleryRepository.findById(galleryId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Gallery tidak ditemukan"));
        verifyGalleryOwnership(gallery, invitation);

        imageStorageService.deleteFile(gallery.getImagePath());
        galleryRepository.delete(gallery);
        log.warn("Gallery entry {} deleted: invitationId={}, clientId={}, by {}",
                galleryId, invitation.getId(), invitation.getClientId(), getCurrentUsername());
    }

    @Override
    @Transactional
    public void reorderGalleries(List<Map<String, Integer>> order) {
        Invitation invitation = getMyInvitationOrThrow();

        for (Map<String, Integer> item : order) {
            Integer id = item.get("id");
            Integer sortOrder = item.get("sortOrder");
            if (id == null || sortOrder == null) {
                throw new AuthException("VALIDATION_ERROR", "Setiap item harus memiliki id dan sortOrder", 400);
            }

            Gallery gallery = galleryRepository.findById(id.longValue())
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.NOT_FOUND, "Gallery ID " + id + " tidak ditemukan"));

            if (!gallery.getInvitationId().equals(invitation.getId())) {
                log.warn("Gallery access denied: clientId={} tried to reorder gallery invitationId={}", invitation.getClientId(), invitation.getId());
                throw new AuthException("FORBIDDEN", "Gallery bukan milik invitation Anda", 403);
            }

            gallery.setSortOrder(sortOrder.shortValue());
            gallery.setUpdatedAt(OffsetDateTime.now());
            galleryRepository.save(gallery);
        }
    }

    @Override
    @Transactional
    public List<GalleryDto> bulkUploadGallery(List<MultipartFile> images, List<String> captions) throws IOException {
        if (images.size() > 20) {
            throw new AuthException("VALIDATION_ERROR", "Maksimal 20 gambar per bulk upload", 400);
        }

        Invitation invitation = getMyInvitationOrThrow();
        List<GalleryDto> created = new ArrayList<>();

        for (int i = 0; i < images.size(); i++) {
            MultipartFile file = images.get(i);
            if (file == null || file.isEmpty()) {
                continue;
            }

            String imagePath = imageStorageService.saveGalleryImage(
                    invitation.getClientId(), invitation.getId(), file);

            String caption = (captions != null && i < captions.size()) ? captions.get(i) : null;

            Gallery gallery = new Gallery();
            gallery.setInvitationId(invitation.getId());
            gallery.setImagePath(imagePath);
            gallery.setCaption(caption);
            gallery.setSortOrder((short) (i + 1));
            gallery.setCreatedAt(OffsetDateTime.now());
            gallery.setUpdatedAt(OffsetDateTime.now());

            galleryRepository.save(gallery);
            created.add(toDto(gallery));
        }

        log.info("Bulk gallery upload: {} images added, invitationId={}, clientId={}, by {}",
                created.size(), invitation.getId(), invitation.getClientId(), getCurrentUsername());
        return created;
    }

    // ===================== PERSON PHOTO =====================

    @Override
    @Transactional
    public String uploadPersonPhoto(Long personId, MultipartFile photoFile) throws IOException {
        Invitation invitation = getMyInvitationOrThrow();

        if (photoFile == null || photoFile.isEmpty()) {
            throw new AuthException("VALIDATION_ERROR", "File foto wajib diupload", 400);
        }

        InvitationPerson person = invitationPersonRepository.findById(personId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Person tidak ditemukan"));

        if (!person.getInvitationId().equals(invitation.getId())) {
            log.warn("Gallery access denied: clientId={} tried to upload photo for person in invitationId={}", invitation.getClientId(), invitation.getId());
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Person tidak ditemukan");
        }

        String oldPath = person.getPhotoPath();
        if (oldPath != null && !oldPath.isBlank()) {
            imageStorageService.deleteFile(oldPath);
        }

        String newPath = imageStorageService.savePersonPhoto(invitation.getClientId(), personId, photoFile);
        person.setPhotoPath(newPath);
        person.setUpdatedAt(OffsetDateTime.now());
        invitationPersonRepository.save(person);

        return newPath;
    }

    @Override
    @Transactional
    public void deletePersonPhoto(Long personId) throws IOException {
        Invitation invitation = getMyInvitationOrThrow();

        InvitationPerson person = invitationPersonRepository.findById(personId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Person tidak ditemukan"));

        if (!person.getInvitationId().equals(invitation.getId())) {
            log.warn("Gallery access denied: clientId={} tried to delete photo for person in invitationId={}", invitation.getClientId(), invitation.getId());
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Person tidak ditemukan");
        }

        String oldPath = person.getPhotoPath();
        if (oldPath != null && !oldPath.isBlank()) {
            imageStorageService.deleteFile(oldPath);
        }

        person.setPhotoPath(null);
        person.setUpdatedAt(OffsetDateTime.now());
        invitationPersonRepository.save(person);
    }

    // ===================== PUBLIC GALLERY =====================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> getPublicGallery(String slug) {
        Invitation invitation = invitationRepository.findBySlug(slug)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Undangan tidak ditemukan"));

        return galleryRepository.findByInvitationId(invitation.getId()).stream()
                .sorted((a, b) -> a.getSortOrder().compareTo(b.getSortOrder()))
                .map(g -> Map.<String, String>of(
                        "imagePath", g.getImagePath(),
                        "caption", g.getCaption()
                ))
                .toList();
    }

    // ===================== HELPERS =====================

    private Short getNextSortOrder(Long invitationId) {
        List<Gallery> galleries = galleryRepository.findByInvitationId(invitationId);
        if (galleries.isEmpty()) {
            return 1;
        }
        return (short) (galleries.stream()
                .mapToInt(g -> g.getSortOrder() != null ? g.getSortOrder() : 0)
                .max()
                .orElse(0) + 1);
    }

    private GalleryDto toDto(Gallery gallery) {
        GalleryDto dto = new GalleryDto();
        dto.setId(gallery.getId());
        dto.setInvitationId(gallery.getInvitationId());
        dto.setImagePath(gallery.getImagePath());
        dto.setCaption(gallery.getCaption());
        dto.setSortOrder(gallery.getSortOrder());
        dto.setCreatedAt(gallery.getCreatedAt());
        dto.setUpdatedAt(gallery.getUpdatedAt());
        return dto;
    }
}
