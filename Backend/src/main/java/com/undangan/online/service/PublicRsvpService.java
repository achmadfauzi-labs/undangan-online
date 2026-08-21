package com.undangan.online.service;

import com.undangan.online.dto.RsvpSubmitRequest;
import com.undangan.online.dto.RsvpUpdateRequest;
import com.undangan.online.entity.Guest;
import com.undangan.online.entity.Invitation;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.GuestRepository;
import com.undangan.online.repository.InvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PublicRsvpService {

    private final GuestRepository guestRepository;
    private final InvitationRepository invitationRepository;

    public PublicRsvpService(GuestRepository guestRepository, InvitationRepository invitationRepository) {
        this.guestRepository = guestRepository;
        this.invitationRepository = invitationRepository;
    }

    @Transactional
    public java.util.Map<String, Object> submitRsvp(RsvpSubmitRequest request) {
        if (!"attending".equals(request.getAttendanceStatus()) && !"not_attending".equals(request.getAttendanceStatus())) {
            throw new AuthException("VALIDATION_ERROR", "Attendance status harus 'attending' atau 'not_attending'", 400);
        }

        Guest guest = guestRepository.findByInvitationToken(request.getToken())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Token tidak valid"));

        if ("attending".equals(request.getAttendanceStatus())) {
            Short partySize = request.getPartySize();
            if (partySize == null || partySize <= 0) {
                partySize = 1;
            }
            guest.setPartySize(partySize);
        } else {
            guest.setPartySize(request.getPartySize() != null ? request.getPartySize() : guest.getPartySize());
        }

        guest.setAttendanceStatus(request.getAttendanceStatus());
        if (request.getMessage() != null) {
            guest.setMessage(request.getMessage());
        }
        guest.setUpdatedAt(OffsetDateTime.now());
        guestRepository.save(guest);

        return java.util.Map.of(
                "success", true,
                "message", "RSVP berhasil tercatat"
        );
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getRsvpStatus(String token) {
        Guest guest = guestRepository.findByInvitationToken(token)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Token tidak valid"));

        return java.util.Map.of(
                "name", guest.getName(),
                "attendanceStatus", guest.getAttendanceStatus(),
                "partySize", guest.getPartySize(),
                "message", guest.getMessage()
        );
    }

    @Transactional
    public java.util.Map<String, Object> submitGuestbook(String slug, String name, String message) {
        if (name == null || name.isBlank()) {
            throw new AuthException("VALIDATION_ERROR", "Nama wajib diisi", 400);
        }
        if (message == null || message.isBlank() || message.length() < 3 || message.length() > 500) {
            throw new AuthException("VALIDATION_ERROR", "Pesan wajib diisi (3-500 karakter)", 400);
        }

        Invitation invitation = invitationRepository.findBySlug(slug)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Undangan tidak ditemukan"));

        Guest guest = new Guest();
        guest.setInvitationId(invitation.getId());
        guest.setInvitationToken(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        guest.setName(name);
        guest.setAttendanceStatus("pending");
        guest.setPartySize((short) 1);
        guest.setMessage(message);
        guest.setIsPublished(true);
        guest.setCreatedAt(OffsetDateTime.now());
        guest.setUpdatedAt(OffsetDateTime.now());

        guestRepository.save(guest);

        return java.util.Map.of(
                "success", true,
                "message", "Ucapan berhasil dikirim"
        );
    }

    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getGuestbook(String slug) {
        Invitation invitation = invitationRepository.findBySlug(slug)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Undangan tidak ditemukan"));

        return guestRepository.findByInvitationIdAndIsPublishedTrue(invitation.getId(),
                        org.springframework.data.domain.Sort.by("createdAt").descending()).stream()
                .map(g -> java.util.Map.<String, Object>of(
                        "name", g.getName(),
                        "message", g.getMessage(),
                        "createdAt", g.getCreatedAt() != null ? g.getCreatedAt().toString() : null
                ))
                .toList();
    }
}
