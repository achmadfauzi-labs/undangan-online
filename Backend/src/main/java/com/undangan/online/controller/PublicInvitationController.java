package com.undangan.online.controller;

import com.undangan.online.entity.Guest;
import com.undangan.online.entity.Invitation;
import com.undangan.online.entity.InvitationPerson;
import com.undangan.online.entity.InvitationSession;
import com.undangan.online.entity.Template;
import com.undangan.online.repository.GuestRepository;
import com.undangan.online.repository.InvitationRepository;
import com.undangan.online.repository.TemplateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/v1/public")
public class PublicInvitationController {

    private final InvitationRepository invitationRepository;
    private final TemplateRepository templateRepository;
    private final GuestRepository guestRepository;

    public PublicInvitationController(InvitationRepository invitationRepository, TemplateRepository templateRepository, GuestRepository guestRepository) {
        this.invitationRepository = invitationRepository;
        this.templateRepository = templateRepository;
        this.guestRepository = guestRepository;
    }

    @GetMapping("/invitation/{slug}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getInvitation(@PathVariable String slug) {
        Invitation invitation = invitationRepository.findBySlug(slug)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Undangan tidak ditemukan"));

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

        Map<String, Object> templateMap = template == null ? null : new HashMap<>();
        if (template != null) {
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

        return ResponseEntity.ok(response);
    }

    @GetMapping("/invitation/{slug}/guestbook")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getGuestbook(@PathVariable String slug) {
        Invitation invitation = invitationRepository.findBySlug(slug)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Undangan tidak ditemukan"));

        List<Guest> guests = guestRepository.findByInvitationIdAndIsPublishedTrue(invitation.getId(), Sort.by("createdAt").descending());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Guest g : guests) {
            Map<String, Object> guestMap = new HashMap<>();
            guestMap.put("name", g.getName());
            guestMap.put("message", g.getMessage());
            guestMap.put("createdAt", g.getCreatedAt() != null ? g.getCreatedAt().toString() : null);
            result.add(guestMap);
        }

        return ResponseEntity.ok(result);
    }
}
