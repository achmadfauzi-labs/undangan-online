package com.undangan.online.service.impl;

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
import com.undangan.online.entity.Guest;
import com.undangan.online.entity.Invitation;
import com.undangan.online.entity.InvitationPerson;
import com.undangan.online.entity.InvitationSession;
import com.undangan.online.entity.LoveStory;
import com.undangan.online.entity.Template;
import com.undangan.online.entity.Users;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.GuestRepository;
import com.undangan.online.repository.InvitationPersonRepository;
import com.undangan.online.repository.InvitationRepository;
import com.undangan.online.repository.InvitationSessionRepository;
import com.undangan.online.repository.LoveStoryRepository;
import com.undangan.online.repository.TemplateRepository;
import com.undangan.online.service.ClientInvitationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class ClientInvitationServiceImpl implements ClientInvitationService {

    private static final Logger log = LoggerFactory.getLogger(ClientInvitationServiceImpl.class);

    private final InvitationRepository invitationRepository;
    private final InvitationPersonRepository invitationPersonRepository;
    private final InvitationSessionRepository invitationSessionRepository;
    private final LoveStoryRepository loveStoryRepository;
    private final TemplateRepository templateRepository;
    private final GuestRepository guestRepository;

    public ClientInvitationServiceImpl(InvitationRepository invitationRepository,
                                       InvitationPersonRepository invitationPersonRepository,
                                       InvitationSessionRepository invitationSessionRepository,
                                       LoveStoryRepository loveStoryRepository,
                                       TemplateRepository templateRepository,
                                       GuestRepository guestRepository) {
        this.invitationRepository = invitationRepository;
        this.invitationPersonRepository = invitationPersonRepository;
        this.invitationSessionRepository = invitationSessionRepository;
        this.loveStoryRepository = loveStoryRepository;
        this.templateRepository = templateRepository;
        this.guestRepository = guestRepository;
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
        Invitation invitation = invitationRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invitation belum dibuat"));
        return invitation;
    }

    private void verifyInvitationOwnership(Invitation invitation) {
        Long clientId = getClientId();
        if (!clientId.equals(invitation.getClientId())) {
            log.warn("Access denied: clientId {} tried to access invitation {}", clientId, invitation.getId());
            throw new AuthException("FORBIDDEN", "Bukan invitation milik Anda", 403);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InvitationDto getMyInvitation() {
        Invitation invitation = getMyInvitationOrThrow();
        return toInvitationDto(invitation);
    }

    @Override
    @Transactional
    public InvitationDto updateInvitation(UpdateInvitationRequest request) {
        Invitation invitation = getMyInvitationOrThrow();
        Long clientId = getClientId();

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

        log.info("Invitation {} updated for client {}", invitation.getId(), clientId);
        return toInvitationDto(invitation);
    }

    @Override
    @Transactional
    public Map<String, String> generateSlug(String baseSlug) {
        Long clientId = getClientId();
        Invitation invitation = invitationRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invitation belum dibuat"));

        String slug = buildSlug(baseSlug);
        while (invitationRepository.findBySlug(slug).isPresent()) {
            slug = buildSlug(baseSlug);
        }

        invitation.setSlug(slug);
        invitation.setUpdatedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);

        log.info("Slug generated: {} for client {}", slug, clientId);
        return Map.of("slug", slug);
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

    @Override
    @Transactional
    public void publishInvitation() {
        Invitation invitation = getMyInvitationOrThrow();
        Long clientId = getClientId();

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

        log.info("Invitation {} published for client {}", invitation.getId(), clientId);
    }

    // ===================== PERSON =====================

    @Override
    @Transactional(readOnly = true)
    public List<InvitationPersonDto> listPersons() {
        Invitation invitation = getMyInvitationOrThrow();
        return invitationPersonRepository.findByInvitationId(invitation.getId()).stream()
                .map(this::toPersonDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InvitationPersonDto getPerson(Long personId) {
        Invitation invitation = getMyInvitationOrThrow();
        InvitationPerson person = invitationPersonRepository.findById(personId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Person tidak ditemukan"));
        verifyInvitationOwnership(invitation);
        if (!person.getInvitationId().equals(invitation.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Person tidak ditemukan");
        }
        return toPersonDto(person);
    }

    @Override
    @Transactional
    public InvitationPersonDto createPerson(CreateInvitationPersonRequest request) {
        Invitation invitation = getMyInvitationOrThrow();
        Long clientId = getClientId();

        if (!"groom".equals(request.getRole()) && !"bride".equals(request.getRole())) {
            throw new AuthException("VALIDATION_ERROR", "Role harus 'groom' atau 'bride'", 400);
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

        try {
            invitationPersonRepository.save(person);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateConstraint(ex)) {
                log.warn("Konflik: role {} sudah ada pada undangan {} oleh client {}",
                        request.getRole(), invitation.getId(), clientId);
                throw new AuthException("CONFLICT",
                        "Role " + request.getRole() + " sudah ada pada undangan ini", 409);
            }
            log.error("Gagal menyimpan person untuk undangan {} oleh client {}", invitation.getId(), clientId, ex);
            throw ex;
        }

        log.info("Person ditambahkan ke undangan {} oleh client {}", invitation.getId(), clientId);
        return toPersonDto(person);
    }

    private boolean isDuplicateConstraint(DataIntegrityViolationException ex) {
        Throwable t = ex;
        while (t != null) {
            if (t.getMessage() != null && t.getMessage().contains("duplicate key value violates unique constraint")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    @Override
    @Transactional
    public InvitationPersonDto updatePerson(Long personId, UpdateInvitationPersonRequest request) {
        Invitation invitation = getMyInvitationOrThrow();
        InvitationPerson person = invitationPersonRepository.findById(personId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Person tidak ditemukan"));

        if (!person.getInvitationId().equals(invitation.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Person tidak ditemukan");
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

    @Override
    @Transactional
    public void deletePerson(Long personId) {
        Invitation invitation = getMyInvitationOrThrow();
        InvitationPerson person = invitationPersonRepository.findById(personId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Person tidak ditemukan"));

        if (!person.getInvitationId().equals(invitation.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Person tidak ditemukan");
        }

        invitationPersonRepository.delete(person);
        log.info("Person {} deleted from invitation {} for client {}", personId, invitation.getId(), getClientId());
    }

    // ===================== SESSION =====================

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public InvitationSessionDto getSession(Long sessionId) {
        Invitation invitation = getMyInvitationOrThrow();
        InvitationSession session = invitationSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session tidak ditemukan"));
        verifyInvitationOwnership(invitation);
        if (!session.getInvitationId().equals(invitation.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session tidak ditemukan");
        }
        return toSessionDto(session);
    }

    @Override
    @Transactional
    public InvitationSessionDto createSession(CreateInvitationSessionRequest request) {
        Invitation invitation = getMyInvitationOrThrow();
        Long clientId = getClientId();

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

        log.info("Session added to invitation {} by client {}", invitation.getId(), clientId);
        return toSessionDto(session);
    }

    @Override
    @Transactional
    public InvitationSessionDto updateSession(Long sessionId, UpdateInvitationSessionRequest request) {
        Invitation invitation = getMyInvitationOrThrow();
        InvitationSession session = invitationSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session tidak ditemukan"));

        if (!session.getInvitationId().equals(invitation.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session tidak ditemukan");
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

    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        Invitation invitation = getMyInvitationOrThrow();
        InvitationSession session = invitationSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session tidak ditemukan"));

        if (!session.getInvitationId().equals(invitation.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session tidak ditemukan");
        }

        invitationSessionRepository.delete(session);
        log.info("Session {} deleted from invitation {} for client {}", sessionId, invitation.getId(), getClientId());
    }

    @Override
    @Transactional
    public void reorderSessions(List<Map<String, Integer>> order) {
        Invitation invitation = getMyInvitationOrThrow();

        for (Map<String, Integer> item : order) {
            Integer id = item.get("id");
            Integer sortOrder = item.get("sortOrder");
            if (id == null || sortOrder == null) {
                throw new AuthException("VALIDATION_ERROR", "Setiap item harus memiliki id dan sortOrder", 400);
            }

            InvitationSession session = invitationSessionRepository.findById(id.longValue())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Session ID " + id + " tidak ditemukan"));

            if (!session.getInvitationId().equals(invitation.getId())) {
                throw new AuthException("FORBIDDEN", "Session bukan milik invitation Anda", 403);
            }

            session.setSortOrder(sortOrder.shortValue());
            session.setUpdatedAt(OffsetDateTime.now());
            invitationSessionRepository.save(session);
        }
        log.info("Sessions reordered for invitation {} by client {}", invitation.getId(), getClientId());
    }

    // ===================== LOVE STORY =====================

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public LoveStoryDto getLoveStory(Long loveStoryId) {
        Invitation invitation = getMyInvitationOrThrow();
        LoveStory loveStory = loveStoryRepository.findById(loveStoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Love story tidak ditemukan"));
        verifyInvitationOwnership(invitation);
        if (!loveStory.getInvitationId().equals(invitation.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Love story tidak ditemukan");
        }
        return toLoveStoryDto(loveStory);
    }

    @Override
    @Transactional
    public LoveStoryDto createLoveStory(CreateLoveStoryRequest request) {
        Invitation invitation = getMyInvitationOrThrow();
        Long clientId = getClientId();

        LoveStory loveStory = new LoveStory();
        loveStory.setInvitationId(invitation.getId());
        loveStory.setTitle(request.getTitle());
        loveStory.setStoryDate(request.getStoryDate());
        loveStory.setDescription(request.getDescription());
        loveStory.setSortOrder(request.getSortOrder());
        loveStory.setCreatedAt(OffsetDateTime.now());
        loveStory.setUpdatedAt(OffsetDateTime.now());
        loveStoryRepository.save(loveStory);

        log.info("LoveStory added to invitation {} by client {}", invitation.getId(), clientId);
        return toLoveStoryDto(loveStory);
    }

    @Override
    @Transactional
    public LoveStoryDto updateLoveStory(Long loveStoryId, UpdateLoveStoryRequest request) {
        Invitation invitation = getMyInvitationOrThrow();
        LoveStory loveStory = loveStoryRepository.findById(loveStoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Love story tidak ditemukan"));

        if (!loveStory.getInvitationId().equals(invitation.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Love story tidak ditemukan");
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

        loveStory.setUpdatedAt(OffsetDateTime.now());
        loveStoryRepository.save(loveStory);
        return toLoveStoryDto(loveStory);
    }

    @Override
    @Transactional
    public void deleteLoveStory(Long loveStoryId) {
        Invitation invitation = getMyInvitationOrThrow();
        LoveStory loveStory = loveStoryRepository.findById(loveStoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Love story tidak ditemukan"));

        if (!loveStory.getInvitationId().equals(invitation.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Love story tidak ditemukan");
        }

        loveStoryRepository.delete(loveStory);
        log.info("LoveStory {} deleted from invitation {} for client {}", loveStoryId, invitation.getId(), getClientId());
    }

    // ===================== PUBLIC =====================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPublicInvitation(String slug) {
        Invitation invitation = invitationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Undangan tidak ditemukan"));

        Template template = null;
        if (invitation.getTemplateId() != null) {
            template = templateRepository.findById(invitation.getTemplateId()).orElse(null);
        }

        List<Map<String, Object>> persons = new ArrayList<>();
        if (invitation.getPersons() != null) {
            for (InvitationPerson p : invitation.getPersons()) {
                Map<String, Object> person = new HashMap<>();
                person.put("role", p.getRole());
                person.put("name", p.getName());
                person.put("nickname", p.getNickname());
                person.put("parentNames", p.getParentNames());
                person.put("photoPath", p.getPhotoPath());
                persons.add(person);
            }
        }

        List<Map<String, Object>> sessions = new ArrayList<>();
        if (invitation.getSessions() != null) {
            for (InvitationSession s : invitation.getSessions()) {
                Map<String, Object> session = new HashMap<>();
                session.put("name", s.getName());
                session.put("sessionDate", s.getSessionDate() != null ? s.getSessionDate().toString() : null);
                session.put("sessionTime", s.getSessionTime());
                session.put("location", s.getLocation());
                session.put("mapsUrl", s.getMapsUrl());
                sessions.add(session);
            }
        }

        Map<String, Object> templateMap = null;
        if (template != null) {
            templateMap = new HashMap<>();
            templateMap.put("code", template.getCode());
            templateMap.put("name", template.getName());
            templateMap.put("folderPath", template.getFolderPath());
            templateMap.put("thumbnailCss", template.getThumbnailCss());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("slug", invitation.getSlug());
        response.put("eventTypeCode", invitation.getEventTypeCode());
        response.put("status", invitation.getStatus());
        response.put("welcomeMessage", invitation.getWelcomeMessage());
        response.put("templateId", invitation.getTemplateId());
        response.put("template", templateMap);
        response.put("persons", persons);
        response.put("sessions", sessions);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPublicGuestbook(String slug) {
        Invitation invitation = invitationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Undangan tidak ditemukan"));

        List<Guest> guests = guestRepository.findByInvitationIdAndIsPublishedTrue(
                invitation.getId(), Sort.by("createdAt").descending());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Guest g : guests) {
            Map<String, Object> guestMap = new HashMap<>();
            guestMap.put("name", g.getName());
            guestMap.put("message", g.getMessage());
            guestMap.put("createdAt", g.getCreatedAt() != null ? g.getCreatedAt().toString() : null);
            result.add(guestMap);
        }
        return result;
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
