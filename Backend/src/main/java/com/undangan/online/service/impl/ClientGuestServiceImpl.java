package com.undangan.online.service.impl;

import com.undangan.online.dto.CreateGuestRequest;
import com.undangan.online.dto.GuestDto;
import com.undangan.online.dto.UpdateGuestRequest;
import com.undangan.online.entity.Guest;
import com.undangan.online.entity.Invitation;
import com.undangan.online.entity.Users;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.GuestRepository;
import com.undangan.online.repository.InvitationRepository;
import com.undangan.online.service.ClientGuestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ClientGuestServiceImpl implements ClientGuestService {

    private static final Logger log = LoggerFactory.getLogger(ClientGuestServiceImpl.class);

    private final GuestRepository guestRepository;
    private final InvitationRepository invitationRepository;

    public ClientGuestServiceImpl(GuestRepository guestRepository, InvitationRepository invitationRepository) {
        this.guestRepository = guestRepository;
        this.invitationRepository = invitationRepository;
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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invitation belum dibuat"));
    }

    private void verifyGuestOwnership(Guest guest, Invitation invitation) {
        if (!invitation.getId().equals(guest.getInvitationId())) {
            log.warn("Access denied: guestId {} not under invitation {} for client {}",
                    guest.getId(), invitation.getId(), getClientId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest tidak ditemukan");
        }
    }

    private String generateUniqueToken(Long invitationId) {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        } while (guestRepository.findByInvitationToken(token).isPresent());
        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuestDto> listGuests(String status, String search) {
        Invitation invitation = getMyInvitationOrThrow();
        List<Guest> guests = guestRepository.findByInvitationId(invitation.getId());

        return guests.stream()
                .filter(g -> status == null || status.isBlank() || status.equals(g.getAttendanceStatus()))
                .filter(g -> search == null || search.isBlank() ||
                        (g.getName() != null && g.getName().toLowerCase().contains(search.toLowerCase())))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GuestDto getGuest(Long guestId) {
        Invitation invitation = getMyInvitationOrThrow();
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Guest tidak ditemukan"));
        verifyGuestOwnership(guest, invitation);
        return toDto(guest);
    }

    @Override
    @Transactional
    public GuestDto createGuest(CreateGuestRequest request) {
        Invitation invitation = getMyInvitationOrThrow();
        Long clientId = getClientId();

        String token = request.getInvitationToken();
        if (token == null || token.isBlank()) {
            token = generateUniqueToken(invitation.getId());
        } else {
            if (guestRepository.findByInvitationToken(token).isPresent()) {
                throw new AuthException("CONFLICT", "Token sudah digunakan", 409);
            }
        }

        Guest guest = new Guest();
        guest.setInvitationId(invitation.getId());
        guest.setInvitationToken(token);
        guest.setName(request.getName());
        guest.setCategoryCode(request.getCategoryCode());
        guest.setEmail(request.getEmail());
        guest.setPhone(request.getPhone());
        guest.setAttendanceStatus(request.getAttendanceStatus() != null ? request.getAttendanceStatus() : "menunggu");
        guest.setPartySize(request.getPartySize() != null ? request.getPartySize() : 1);
        guest.setIsPublished(true);
        guest.setMessage(null);
        guest.setReply(null);
        guest.setCreatedAt(OffsetDateTime.now());
        guest.setUpdatedAt(OffsetDateTime.now());

        try {
            guestRepository.save(guest);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("duplicate key value violates unique constraint")) {
                throw new AuthException("CONFLICT", "Token sudah digunakan", 409);
            }
            log.error("Failed to save guest for invitation {} by client {}", invitation.getId(), clientId, ex);
            throw ex;
        }

        int nameLength = request.getName() != null ? request.getName().length() : 0;
        log.info("Guest created: invitationId={}, nameLength={}, by clientId={}", invitation.getId(), nameLength, clientId);
        return toDto(guest);
    }

    @Override
    @Transactional
    public GuestDto updateGuest(Long guestId, UpdateGuestRequest request) {
        Invitation invitation = getMyInvitationOrThrow();
        Long clientId = getClientId();
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Guest tidak ditemukan"));
        verifyGuestOwnership(guest, invitation);

        if (request.getName() != null) {
            guest.setName(request.getName());
        }
        if (request.getCategoryCode() != null) {
            guest.setCategoryCode(request.getCategoryCode());
        }
        if (request.getEmail() != null) {
            guest.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            guest.setPhone(request.getPhone());
        }
        if (request.getPartySize() != null) {
            guest.setPartySize(request.getPartySize());
        }

        guest.setUpdatedAt(OffsetDateTime.now());
        guestRepository.save(guest);

        log.info("Guest {} updated for invitation {} by clientId={}", guestId, invitation.getId(), clientId);
        return toDto(guest);
    }

    @Override
    @Transactional
    public void deleteGuest(Long guestId) {
        Invitation invitation = getMyInvitationOrThrow();
        Long clientId = getClientId();
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Guest tidak ditemukan"));
        verifyGuestOwnership(guest, invitation);
        guestRepository.delete(guest);

        log.warn("Guest deleted: id={}, invitationId={}, by clientId={}", guestId, invitation.getId(), clientId);
    }

    @Override
    @Transactional
    public List<GuestDto> bulkCreateGuests(List<CreateGuestRequest> requests) {
        Invitation invitation = getMyInvitationOrThrow();
        Long clientId = getClientId();
        if (requests.size() > 100) {
            throw new AuthException("VALIDATION_ERROR", "Maksimal 100 guests per bulk create", 400);
        }

        List<GuestDto> created = new ArrayList<>();
        for (CreateGuestRequest req : requests) {
            created.add(createGuest(req));
        }

        log.info("Bulk guest import: invitationId={}, count={}, by clientId={}", invitation.getId(), created.size(), clientId);
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generateGuestLink(Long guestId) {
        Invitation invitation = getMyInvitationOrThrow();
        Long clientId = getClientId();
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Guest tidak ditemukan"));
        verifyGuestOwnership(guest, invitation);

        String baseUrl = "http://localhost:3000";
        String fullUrl = String.format("%s/undangan/%s?to=%s&token=%s",
                baseUrl,
                invitation.getSlug() != null ? invitation.getSlug() : "",
                java.net.URLEncoder.encode(guest.getName(), java.nio.charset.StandardCharsets.UTF_8),
                guest.getInvitationToken());

        log.info("Guest link generated: guestId={}, invitationId={}, by clientId={}", guestId, invitation.getId(), clientId);
        return Map.of(
                "baseUrl", baseUrl,
                "fullUrl", fullUrl
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuestDto> exportGuests() {
        Invitation invitation = getMyInvitationOrThrow();
        return guestRepository.findByInvitationId(invitation.getId()).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toDto)
                .toList();
    }

    private GuestDto toDto(Guest guest) {
        GuestDto dto = new GuestDto();
        dto.setId(guest.getId());
        dto.setInvitationId(guest.getInvitationId());
        dto.setInvitationToken(guest.getInvitationToken());
        dto.setName(guest.getName());
        dto.setCategoryCode(guest.getCategoryCode());
        dto.setEmail(guest.getEmail());
        dto.setPhone(guest.getPhone());
        dto.setAttendanceStatus(guest.getAttendanceStatus());
        dto.setPartySize(guest.getPartySize());
        dto.setMessage(guest.getMessage());
        dto.setIsPublished(guest.getIsPublished());
        dto.setReply(guest.getReply());
        dto.setCreatedAt(guest.getCreatedAt());
        dto.setUpdatedAt(guest.getUpdatedAt());
        return dto;
    }
}
