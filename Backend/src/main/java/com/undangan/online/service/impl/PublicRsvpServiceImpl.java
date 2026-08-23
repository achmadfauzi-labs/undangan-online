package com.undangan.online.service.impl;

import com.undangan.online.dto.RsvpSubmitRequest;
import com.undangan.online.entity.Guest;
import com.undangan.online.entity.Invitation;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.GuestRepository;
import com.undangan.online.repository.InvitationRepository;
import com.undangan.online.service.PublicRsvpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PublicRsvpServiceImpl implements PublicRsvpService {

    private static final Logger log = LoggerFactory.getLogger(PublicRsvpServiceImpl.class);

    private final GuestRepository guestRepository;
    private final InvitationRepository invitationRepository;

    public PublicRsvpServiceImpl(GuestRepository guestRepository, InvitationRepository invitationRepository) {
        this.guestRepository = guestRepository;
        this.invitationRepository = invitationRepository;
    }

    @Override
    @Transactional
    public Map<String, Object> submitRsvp(RsvpSubmitRequest request) {
        if (!"hadir".equals(request.getAttendanceStatus())
                && !"tidak_hadir".equals(request.getAttendanceStatus())
                && !"ragu".equals(request.getAttendanceStatus())) {
            throw new AuthException("VALIDATION_ERROR", "Attendance status harus 'hadir', 'tidak_hadir', atau 'ragu'", 400);
        }

        Guest guest = guestRepository.findByInvitationToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Token tidak valid"));

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
        guest.setUpdatedAt(java.time.OffsetDateTime.now());
        guestRepository.save(guest);

        log.info("RSVP submitted: invitationId={}, attendance={}", guest.getInvitationId(), request.getAttendanceStatus());
        return Map.of(
                "success", true,
                "message", "RSVP berhasil tercatat"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getRsvpStatus(String token) {
        Guest guest = guestRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Token tidak valid"));

        return Map.of(
                "name", guest.getName(),
                "attendanceStatus", guest.getAttendanceStatus(),
                "partySize", guest.getPartySize(),
                "message", guest.getMessage()
        );
    }

    @Override
    @Transactional
    public Map<String, Object> submitGuestbook(String slug, String name, String message) {
        if (name == null || name.isBlank()) {
            throw new AuthException("VALIDATION_ERROR", "Nama wajib diisi", 400);
        }
        if (message == null || message.isBlank() || message.length() < 3 || message.length() > 500) {
            throw new AuthException("VALIDATION_ERROR", "Pesan wajib diisi (3-500 karakter)", 400);
        }

        Invitation invitation = invitationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Undangan tidak ditemukan"));

        Guest guest = new Guest();
        guest.setInvitationId(invitation.getId());
        guest.setInvitationToken(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
        guest.setName(name);
        guest.setAttendanceStatus("menunggu");
        guest.setPartySize((short) 1);
        guest.setMessage(message);
        guest.setIsPublished(true);
        guest.setCreatedAt(java.time.OffsetDateTime.now());
        guest.setUpdatedAt(java.time.OffsetDateTime.now());

        guestRepository.save(guest);

        int nameLength = name != null ? name.length() : 0;
        log.info("Guestbook entry submitted: id={}, nameLength={}", guest.getId(), nameLength);
        return Map.of(
                "success", true,
                "message", "Ucapan berhasil dikirim"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGuestbook(String slug) {
        Invitation invitation = invitationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Undangan tidak ditemukan"));

        return guestRepository.findByInvitationIdAndIsPublishedTrue(invitation.getId(),
                org.springframework.data.domain.Sort.by("createdAt").descending()).stream()
                .filter(g -> g.getMessage() != null)
                .map(g -> java.util.Map.<String, Object>of(
                        "name", g.getName(),
                        "message", g.getMessage(),
                        "createdAt", g.getCreatedAt() != null ? g.getCreatedAt().toString() : ""
                ))
                .toList();
    }
}
