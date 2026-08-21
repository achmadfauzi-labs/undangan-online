package com.undangan.online.service;

import com.undangan.online.dto.CreateGuestRequest;
import com.undangan.online.dto.GuestDto;
import com.undangan.online.dto.UpdateGuestRequest;
import com.undangan.online.entity.Guest;
import com.undangan.online.entity.Invitation;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.GuestRepository;
import com.undangan.online.repository.InvitationRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ClientGuestService {

    private final GuestRepository guestRepository;
    private final InvitationRepository invitationRepository;

    public ClientGuestService(GuestRepository guestRepository, InvitationRepository invitationRepository) {
        this.guestRepository = guestRepository;
        this.invitationRepository = invitationRepository;
    }

    private Long getClientId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthException("UNAUTHORIZED", "Tidak terautentikasi", 401);
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof com.undangan.online.entity.Users user) {
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

    private void verifyGuestOwnership(Guest guest, Invitation invitation) {
        if (!invitation.getId().equals(guest.getInvitationId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Guest tidak ditemukan");
        }
    }

    private String generateUniqueToken(Long invitationId) {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        } while (guestRepository.findByInvitationToken(token).isPresent());
        return token;
    }

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

    @Transactional(readOnly = true)
    public GuestDto getGuest(Long guestId) {
        Invitation invitation = getMyInvitationOrThrow();
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Guest tidak ditemukan"));
        verifyGuestOwnership(guest, invitation);
        return toDto(guest);
    }

    @Transactional
    public GuestDto createGuest(CreateGuestRequest request) {
        Invitation invitation = getMyInvitationOrThrow();

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

        guestRepository.save(guest);
        return toDto(guest);
    }

    @Transactional
    public GuestDto updateGuest(Long guestId, UpdateGuestRequest request) {
        Invitation invitation = getMyInvitationOrThrow();
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Guest tidak ditemukan"));
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
        return toDto(guest);
    }

    @Transactional
    public void deleteGuest(Long guestId) {
        Invitation invitation = getMyInvitationOrThrow();
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Guest tidak ditemukan"));
        verifyGuestOwnership(guest, invitation);
        guestRepository.delete(guest);
    }

    @Transactional
    public List<GuestDto> bulkCreateGuests(List<CreateGuestRequest> requests) {
        if (requests.size() > 100) {
            throw new AuthException("VALIDATION_ERROR", "Maksimal 100 guests per bulk create", 400);
        }

        List<GuestDto> created = new ArrayList<>();
        for (CreateGuestRequest req : requests) {
            created.add(createGuest(req));
        }
        return created;
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> generateGuestLink(Long guestId) {
        Invitation invitation = getMyInvitationOrThrow();
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Guest tidak ditemukan"));
        verifyGuestOwnership(guest, invitation);

        String baseUrl = "http://localhost:3000";
        String fullUrl = String.format("%s/undangan/%s?to=%s&token=%s",
                baseUrl,
                invitation.getSlug() != null ? invitation.getSlug() : "",
                java.net.URLEncoder.encode(guest.getName(), java.nio.charset.StandardCharsets.UTF_8),
                guest.getInvitationToken());

        return java.util.Map.of(
                "baseUrl", baseUrl,
                "fullUrl", fullUrl
        );
    }

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
