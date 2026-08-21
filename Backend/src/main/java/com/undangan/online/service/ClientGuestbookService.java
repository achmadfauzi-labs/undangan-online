package com.undangan.online.service;

import com.undangan.online.dto.GuestbookModerationDto;
import com.undangan.online.entity.Guest;
import com.undangan.online.entity.Invitation;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.GuestRepository;
import com.undangan.online.repository.InvitationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientGuestbookService {

    private final GuestRepository guestRepository;
    private final InvitationRepository invitationRepository;

    public ClientGuestbookService(GuestRepository guestRepository, InvitationRepository invitationRepository) {
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

    @Transactional(readOnly = true)
    public List<com.undangan.online.dto.GuestbookEntryDto> listEntries(Boolean isPublished, String search) {
        Invitation invitation = getMyInvitationOrThrow();
        List<Guest> guests = guestRepository.findByInvitationId(invitation.getId());

        return guests.stream()
                .filter(g -> isPublished == null || isPublished.equals(g.getIsPublished()))
                .filter(g -> search == null || search.isBlank() ||
                        (g.getName() != null && g.getName().toLowerCase().contains(search.toLowerCase())))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toGuestbookEntryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public com.undangan.online.dto.GuestbookEntryDto getEntry(Long guestId) {
        Invitation invitation = getMyInvitationOrThrow();
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Guest tidak ditemukan"));
        verifyGuestOwnership(guest, invitation);
        return toGuestbookEntryDto(guest);
    }

    @Transactional
    public com.undangan.online.dto.GuestbookEntryDto replyToEntry(Long guestId, GuestbookModerationDto request) {
        Invitation invitation = getMyInvitationOrThrow();
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Guest tidak ditemukan"));
        verifyGuestOwnership(guest, invitation);

        guest.setReply(request.getReply());
        guest.setUpdatedAt(OffsetDateTime.now());
        guestRepository.save(guest);

        return toGuestbookEntryDto(guest);
    }

    @Transactional
    public void deleteEntry(Long guestId) {
        Invitation invitation = getMyInvitationOrThrow();
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Guest tidak ditemukan"));
        verifyGuestOwnership(guest, invitation);
        guestRepository.delete(guest);
    }

    private com.undangan.online.dto.GuestbookEntryDto toGuestbookEntryDto(Guest guest) {
        com.undangan.online.dto.GuestbookEntryDto dto = new com.undangan.online.dto.GuestbookEntryDto();
        dto.setId(guest.getId());
        dto.setInvitationId(guest.getInvitationId());
        dto.setName(guest.getName());
        dto.setMessage(guest.getMessage());
        dto.setIsPublished(guest.getIsPublished());
        dto.setAttendanceStatus(guest.getAttendanceStatus());
        dto.setReply(guest.getReply());
        dto.setCreatedAt(guest.getCreatedAt());
        dto.setUpdatedAt(guest.getUpdatedAt());
        return dto;
    }
}
