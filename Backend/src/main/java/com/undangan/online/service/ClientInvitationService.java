package com.undangan.online.service;

import com.undangan.online.dto.CreateInvitationPersonRequest;
import com.undangan.online.dto.CreateInvitationSessionRequest;
import com.undangan.online.dto.CreateLoveStoryRequest;
import com.undangan.online.dto.InvitationDto;
import com.undangan.online.dto.InvitationPersonDto;
import com.undangan.online.dto.InvitationSessionDto;
import com.undangan.online.dto.LoveStoryDto;
import com.undangan.online.dto.UpdateInvitationPersonRequest;
import com.undangan.online.dto.UpdateInvitationRequest;
import com.undangan.online.dto.UpdateInvitationSessionRequest;
import com.undangan.online.dto.UpdateLoveStoryRequest;
import com.undangan.online.entity.Invitation;
import com.undangan.online.entity.InvitationPerson;
import com.undangan.online.entity.InvitationSession;
import com.undangan.online.entity.LoveStory;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.InvitationPersonRepository;
import com.undangan.online.repository.InvitationRepository;
import com.undangan.online.repository.InvitationSessionRepository;
import com.undangan.online.repository.LoveStoryRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class ClientInvitationService {

    private final InvitationRepository invitationRepository;
    private final InvitationPersonRepository invitationPersonRepository;
    private final InvitationSessionRepository invitationSessionRepository;
    private final LoveStoryRepository loveStoryRepository;

    public ClientInvitationService(InvitationRepository invitationRepository,
                                   InvitationPersonRepository invitationPersonRepository,
                                   InvitationSessionRepository invitationSessionRepository,
                                   LoveStoryRepository loveStoryRepository) {
        this.invitationRepository = invitationRepository;
        this.invitationPersonRepository = invitationPersonRepository;
        this.invitationSessionRepository = invitationSessionRepository;
        this.loveStoryRepository = loveStoryRepository;
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
        Invitation invitation = invitationRepository.findByClientId(clientId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Invitation belum dibuat"));
        return invitation;
    }

    private void verifyInvitationOwnership(Invitation invitation) {
        Long clientId = getClientId();
        if (!clientId.equals(invitation.getClientId())) {
            throw new AuthException("FORBIDDEN", "Bukan invitation milik Anda", 403);
        }
    }

    @Transactional(readOnly = true)
    public InvitationDto getMyInvitation() {
        Invitation invitation = getMyInvitationOrThrow();
        return toInvitationDto(invitation);
    }

    @Transactional
    public InvitationDto updateInvitation(UpdateInvitationRequest request) {
        Invitation invitation = getMyInvitationOrThrow();

        if (request.getWelcomeMessage() != null) {
            invitation.setWelcomeMessage(request.getWelcomeMessage());
        }
        if (request.getEventTypeCode() != null) {
            invitation.setEventTypeCode(request.getEventTypeCode());
        }
        if (request.getStatus() != null) {
            invitation.setStatus(request.getStatus());
        }
        invitation.setUpdatedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);

        return toInvitationDto(invitation);
    }

    @Transactional
    public java.util.Map<String, String> generateSlug(String baseSlug) {
        Long clientId = getClientId();
        Invitation invitation = invitationRepository.findByClientId(clientId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Invitation belum dibuat"));

        String slug = buildSlug(baseSlug);
        while (invitationRepository.findBySlug(slug).isPresent()) {
            slug = buildSlug(baseSlug);
        }

        invitation.setSlug(slug);
        invitation.setUpdatedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);

        return java.util.Map.of("slug", slug);
    }

    private String buildSlug(String baseSlug) {
        String raw = (baseSlug == null || baseSlug.isBlank()) ? "undangan" : baseSlug;
        String cleaned = raw.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        if (cleaned.length() > 80) {
            cleaned = cleaned.substring(0, 80);
        }
        String suffix = generateRandomSuffix(4);
        return cleaned + "-" + suffix;
    }

    private String generateRandomSuffix(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Transactional
    public void publishInvitation() {
        Invitation invitation = getMyInvitationOrThrow();

        List<InvitationSession> sessions = invitationSessionRepository.findByInvitationId(invitation.getId());
        List<InvitationPerson> persons = invitationPersonRepository.findByInvitationId(invitation.getId());

        if (sessions.isEmpty()) {
            throw new AuthException("VALIDATION_ERROR", "Undangan belum memiliki sesi acara", 400);
        }
        if (persons.isEmpty()) {
            throw new AuthException("VALIDATION_ERROR", "Undangan belum memiliki data pasangan", 400);
        }

        invitation.setStatus("publikasi");
        invitation.setPublishedAt(OffsetDateTime.now());
        invitation.setUpdatedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);
    }

    // ===================== PERSON =====================

    @Transactional(readOnly = true)
    public List<InvitationPersonDto> listPersons() {
        Invitation invitation = getMyInvitationOrThrow();
        return invitationPersonRepository.findByInvitationId(invitation.getId()).stream()
                .map(this::toPersonDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvitationPersonDto getPerson(Long personId) {
        Invitation invitation = getMyInvitationOrThrow();
        InvitationPerson person = invitationPersonRepository.findById(personId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Person tidak ditemukan"));
        verifyInvitationOwnership(invitation);
        if (!person.getInvitationId().equals(invitation.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Person tidak ditemukan");
        }
        return toPersonDto(person);
    }

    @Transactional
    public InvitationPersonDto createPerson(CreateInvitationPersonRequest request) {
        Invitation invitation = getMyInvitationOrThrow();

        if (!"pria".equals(request.getRole()) && !"wanita".equals(request.getRole())) {
            throw new AuthException("VALIDATION_ERROR", "Role harus 'pria' atau 'wanita'", 400);
        }

        InvitationPerson person = new InvitationPerson();
        person.setInvitationId(invitation.getId());
        person.setRole(request.getRole());
        person.setName(request.getName());
        person.setNickname(request.getNickname());
        person.setParentNames(request.getParentNames());
        person.setChildOrder(request.getChildOrder());
        person.setPhotoPath(request.getPhotoPath());
        person.setSortOrder(request.getSortOrder());
        person.setCreatedAt(OffsetDateTime.now());
        person.setUpdatedAt(OffsetDateTime.now());

        invitationPersonRepository.save(person);
        return toPersonDto(person);
    }

    @Transactional
    public InvitationPersonDto updatePerson(Long personId, UpdateInvitationPersonRequest request) {
        Invitation invitation = getMyInvitationOrThrow();
        InvitationPerson person = invitationPersonRepository.findById(personId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Person tidak ditemukan"));

        if (!person.getInvitationId().equals(invitation.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Person tidak ditemukan");
        }

        if (request.getName() != null) {
            person.setName(request.getName());
        }
        if (request.getNickname() != null) {
            person.setNickname(request.getNickname());
        }
        if (request.getParentNames() != null) {
            person.setParentNames(request.getParentNames());
        }
        if (request.getChildOrder() != null) {
            person.setChildOrder(request.getChildOrder());
        }
        if (request.getPhotoPath() != null) {
            person.setPhotoPath(request.getPhotoPath());
        }
        if (request.getSortOrder() != null) {
            person.setSortOrder(request.getSortOrder());
        }

        person.setUpdatedAt(OffsetDateTime.now());
        invitationPersonRepository.save(person);
        return toPersonDto(person);
    }

    @Transactional
    public void deletePerson(Long personId) {
        Invitation invitation = getMyInvitationOrThrow();
        InvitationPerson person = invitationPersonRepository.findById(personId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Person tidak ditemukan"));

        if (!person.getInvitationId().equals(invitation.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Person tidak ditemukan");
        }

        invitationPersonRepository.delete(person);
    }

    // ===================== SESSION =====================

    @Transactional(readOnly = true)
    public List<InvitationSessionDto> listSessions() {
        Invitation invitation = getMyInvitationOrThrow();
        return invitationSessionRepository.findByInvitationId(invitation.getId()).stream()
                .sorted((a, b) -> {
                    if (a.getSortOrder() == null && b.getSortOrder() == null) return 0;
                    if (a.getSortOrder() == null) return 1;
                    if (b.getSortOrder() == null) return -1;
                    return a.getSortOrder().compareTo(b.getSortOrder());
                })
                .map(this::toSessionDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvitationSessionDto getSession(Long sessionId) {
        Invitation invitation = getMyInvitationOrThrow();
        InvitationSession session = invitationSessionRepository.findById(sessionId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Session tidak ditemukan"));
        if (!session.getInvitationId().equals(invitation.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Session tidak ditemukan");
        }
        return toSessionDto(session);
    }

    @Transactional
    public InvitationSessionDto createSession(CreateInvitationSessionRequest request) {
        Invitation invitation = getMyInvitationOrThrow();

        InvitationSession session = new InvitationSession();
        session.setInvitationId(invitation.getId());
        session.setName(request.getName());
        session.setSessionDate(request.getSessionDate());
        session.setSessionTime(request.getSessionTime());
        session.setLocation(request.getLocation());
        session.setMapsUrl(request.getMapsUrl());
        session.setSortOrder(request.getSortOrder());
        session.setCreatedAt(OffsetDateTime.now());
        session.setUpdatedAt(OffsetDateTime.now());

        invitationSessionRepository.save(session);
        return toSessionDto(session);
    }

    @Transactional
    public InvitationSessionDto updateSession(Long sessionId, UpdateInvitationSessionRequest request) {
        Invitation invitation = getMyInvitationOrThrow();
        InvitationSession session = invitationSessionRepository.findById(sessionId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Session tidak ditemukan"));

        if (!session.getInvitationId().equals(invitation.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Session tidak ditemukan");
        }

        if (request.getName() != null) {
            session.setName(request.getName());
        }
        if (request.getSessionDate() != null) {
            session.setSessionDate(request.getSessionDate());
        }
        if (request.getSessionTime() != null) {
            session.setSessionTime(request.getSessionTime());
        }
        if (request.getLocation() != null) {
            session.setLocation(request.getLocation());
        }
        if (request.getMapsUrl() != null) {
            session.setMapsUrl(request.getMapsUrl());
        }
        if (request.getSortOrder() != null) {
            session.setSortOrder(request.getSortOrder());
        }

        session.setUpdatedAt(OffsetDateTime.now());
        invitationSessionRepository.save(session);
        return toSessionDto(session);
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        Invitation invitation = getMyInvitationOrThrow();
        InvitationSession session = invitationSessionRepository.findById(sessionId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Session tidak ditemukan"));

        if (!session.getInvitationId().equals(invitation.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Session tidak ditemukan");
        }

        invitationSessionRepository.delete(session);
    }

    @Transactional
    public void reorderSessions(java.util.List<java.util.Map<String, Integer>> order) {
        Invitation invitation = getMyInvitationOrThrow();

        for (java.util.Map<String, Integer> item : order) {
            Integer id = item.get("id");
            Integer sortOrder = item.get("sortOrder");
            if (id == null || sortOrder == null) {
                throw new AuthException("VALIDATION_ERROR", "Setiap item harus memiliki id dan sortOrder", 400);
            }

            InvitationSession session = invitationSessionRepository.findById(id.longValue())
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.NOT_FOUND, "Session ID " + id + " tidak ditemukan"));

            if (!session.getInvitationId().equals(invitation.getId())) {
                throw new AuthException("FORBIDDEN", "Session bukan milik invitation Anda", 403);
            }

            session.setSortOrder(sortOrder.shortValue());
            session.setUpdatedAt(OffsetDateTime.now());
            invitationSessionRepository.save(session);
        }
    }

    // ===================== LOVE STORY =====================

    @Transactional(readOnly = true)
    public List<LoveStoryDto> listLoveStories() {
        Invitation invitation = getMyInvitationOrThrow();
        return loveStoryRepository.findByInvitationId(invitation.getId()).stream()
                .sorted((a, b) -> {
                    if (a.getSortOrder() == null && b.getSortOrder() == null) return 0;
                    if (a.getSortOrder() == null) return 1;
                    if (b.getSortOrder() == null) return -1;
                    return a.getSortOrder().compareTo(b.getSortOrder());
                })
                .map(this::toLoveStoryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoveStoryDto getLoveStory(Long loveStoryId) {
        Invitation invitation = getMyInvitationOrThrow();
        LoveStory loveStory = loveStoryRepository.findById(loveStoryId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Love story tidak ditemukan"));
        if (!loveStory.getInvitationId().equals(invitation.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Love story tidak ditemukan");
        }
        return toLoveStoryDto(loveStory);
    }

    @Transactional
    public LoveStoryDto createLoveStory(CreateLoveStoryRequest request) {
        Invitation invitation = getMyInvitationOrThrow();

        LoveStory loveStory = new LoveStory();
        loveStory.setInvitationId(invitation.getId());
        loveStory.setTitle(request.getTitle());
        loveStory.setStoryDate(request.getStoryDate());
        loveStory.setDescription(request.getDescription());
        loveStory.setSortOrder(request.getSortOrder());
        loveStory.setCreatedAt(OffsetDateTime.now());
        loveStory.setUpdatedAt(OffsetDateTime.now());
        loveStoryRepository.save(loveStory);
        return toLoveStoryDto(loveStory);
    }

    @Transactional
    public LoveStoryDto updateLoveStory(Long loveStoryId, UpdateLoveStoryRequest request) {
        Invitation invitation = getMyInvitationOrThrow();
        LoveStory loveStory = loveStoryRepository.findById(loveStoryId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Love story tidak ditemukan"));

        if (!loveStory.getInvitationId().equals(invitation.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Love story tidak ditemukan");
        }

        if (request.getTitle() != null) {
            loveStory.setTitle(request.getTitle());
        }
        if (request.getStoryDate() != null) {
            loveStory.setStoryDate(request.getStoryDate());
        }
        if (request.getDescription() != null) {
            loveStory.setDescription(request.getDescription());
        }
        if (request.getSortOrder() != null) {
            loveStory.setSortOrder(request.getSortOrder());
        }

        loveStoryRepository.save(loveStory);
        return toLoveStoryDto(loveStory);
    }

    @Transactional
    public void deleteLoveStory(Long loveStoryId) {
        Invitation invitation = getMyInvitationOrThrow();
        LoveStory loveStory = loveStoryRepository.findById(loveStoryId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Love story tidak ditemukan"));

        if (!loveStory.getInvitationId().equals(invitation.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Love story tidak ditemukan");
        }

        loveStoryRepository.delete(loveStory);
    }

    // ===================== MAPPERS =====================

    private InvitationDto toInvitationDto(Invitation invitation) {
        InvitationDto dto = new InvitationDto();
        dto.setId(invitation.getId());
        dto.setClientId(invitation.getClientId());
        dto.setSlug(invitation.getSlug());
        dto.setEventTypeCode(invitation.getEventTypeCode());
        dto.setStatus(invitation.getStatus());
        dto.setWelcomeMessage(invitation.getWelcomeMessage());
        dto.setCoverImagePath(invitation.getCoverImagePath());
        dto.setTemplateId(invitation.getTemplateId());
        dto.setPrimaryMusicId(invitation.getPrimaryMusicId());
        dto.setCustomMusicPath(invitation.getCustomMusicPath());
        dto.setCustomMusicTitle(invitation.getCustomMusicTitle());
        dto.setPublishedAt(invitation.getPublishedAt());
        dto.setCreatedAt(invitation.getCreatedAt());
        dto.setUpdatedAt(invitation.getUpdatedAt());

        List<InvitationPerson> persons = invitationPersonRepository.findByInvitationId(invitation.getId());
        dto.setPersons(persons.stream().map(this::toPersonDto).collect(Collectors.toList()));

        List<InvitationSession> sessions = invitationSessionRepository.findByInvitationId(invitation.getId());
        dto.setSessions(sessions.stream().map(this::toSessionDto).collect(Collectors.toList()));

        List<LoveStory> loveStories = loveStoryRepository.findByInvitationId(invitation.getId());
        dto.setLoveStories(loveStories.stream().map(this::toLoveStoryDto).collect(Collectors.toList()));

        return dto;
    }

    private InvitationPersonDto toPersonDto(InvitationPerson person) {
        InvitationPersonDto dto = new InvitationPersonDto();
        dto.setId(person.getId());
        dto.setInvitationId(person.getInvitationId());
        dto.setRole(person.getRole());
        dto.setName(person.getName());
        dto.setNickname(person.getNickname());
        dto.setParentNames(person.getParentNames());
        dto.setChildOrder(person.getChildOrder());
        dto.setPhotoPath(person.getPhotoPath());
        dto.setSortOrder(person.getSortOrder());
        dto.setCreatedAt(person.getCreatedAt());
        dto.setUpdatedAt(person.getUpdatedAt());
        return dto;
    }

    private InvitationSessionDto toSessionDto(InvitationSession session) {
        InvitationSessionDto dto = new InvitationSessionDto();
        dto.setId(session.getId());
        dto.setInvitationId(session.getInvitationId());
        dto.setName(session.getName());
        dto.setSessionDate(session.getSessionDate());
        dto.setSessionTime(session.getSessionTime());
        dto.setLocation(session.getLocation());
        dto.setMapsUrl(session.getMapsUrl());
        dto.setSortOrder(session.getSortOrder());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());
        return dto;
    }

    private LoveStoryDto toLoveStoryDto(LoveStory loveStory) {
        LoveStoryDto dto = new LoveStoryDto();
        dto.setId(loveStory.getId());
        dto.setInvitationId(loveStory.getInvitationId());
        dto.setTitle(loveStory.getTitle());
        dto.setStoryDate(loveStory.getStoryDate());
        dto.setDescription(loveStory.getDescription());
        dto.setSortOrder(loveStory.getSortOrder());
        dto.setCreatedAt(loveStory.getCreatedAt());
        dto.setUpdatedAt(loveStory.getUpdatedAt());
        return dto;
    }
}
