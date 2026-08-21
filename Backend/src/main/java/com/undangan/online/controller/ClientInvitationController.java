package com.undangan.online.controller;

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
import com.undangan.online.service.ClientInvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/client/invitation")
@PreAuthorize("hasRole('USER')")
public class ClientInvitationController {

    private final ClientInvitationService clientInvitationService;

    public ClientInvitationController(ClientInvitationService clientInvitationService) {
        this.clientInvitationService = clientInvitationService;
    }

    @GetMapping
    public ResponseEntity<InvitationDto> getMyInvitation() {
        return ResponseEntity.ok(clientInvitationService.getMyInvitation());
    }

    @PutMapping
    public ResponseEntity<InvitationDto> updateInvitation(@Valid @RequestBody UpdateInvitationRequest request) {
        return ResponseEntity.ok(clientInvitationService.updateInvitation(request));
    }

    @PostMapping("/slug/generate")
    public ResponseEntity<Map<String, String>> generateSlug(@RequestBody(required = false) Map<String, String> body) {
        String baseSlug = body != null ? body.get("baseSlug") : null;
        return ResponseEntity.ok(clientInvitationService.generateSlug(baseSlug));
    }

    @PostMapping("/publish")
    public ResponseEntity<Void> publishInvitation() {
        clientInvitationService.publishInvitation();
        return ResponseEntity.ok().build();
    }

    // ===================== PERSON =====================

    @GetMapping("/persons")
    public ResponseEntity<List<InvitationPersonDto>> listPersons() {
        return ResponseEntity.ok(clientInvitationService.listPersons());
    }

    @GetMapping("/persons/{id}")
    public ResponseEntity<InvitationPersonDto> getPerson(@PathVariable Long id) {
        return ResponseEntity.ok(clientInvitationService.getPerson(id));
    }

    @PostMapping("/persons")
    public ResponseEntity<InvitationPersonDto> createPerson(@Valid @RequestBody CreateInvitationPersonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientInvitationService.createPerson(request));
    }

    @PutMapping("/persons/{id}")
    public ResponseEntity<InvitationPersonDto> updatePerson(@PathVariable Long id,
                                                             @RequestBody UpdateInvitationPersonRequest request) {
        return ResponseEntity.ok(clientInvitationService.updatePerson(id, request));
    }

    @DeleteMapping("/persons/{id}")
    public ResponseEntity<Void> deletePerson(@PathVariable Long id) {
        clientInvitationService.deletePerson(id);
        return ResponseEntity.noContent().build();
    }

    // ===================== SESSION =====================

    @GetMapping("/sessions")
    public ResponseEntity<List<InvitationSessionDto>> listSessions() {
        return ResponseEntity.ok(clientInvitationService.listSessions());
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<InvitationSessionDto> getSession(@PathVariable Long id) {
        return ResponseEntity.ok(clientInvitationService.getSession(id));
    }

    @PostMapping("/sessions")
    public ResponseEntity<InvitationSessionDto> createSession(@Valid @RequestBody CreateInvitationSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientInvitationService.createSession(request));
    }

    @PutMapping("/sessions/{id}")
    public ResponseEntity<InvitationSessionDto> updateSession(@PathVariable Long id,
                                                               @RequestBody UpdateInvitationSessionRequest request) {
        return ResponseEntity.ok(clientInvitationService.updateSession(id, request));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        clientInvitationService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/sessions/reorder")
    public ResponseEntity<Void> reorderSessions(@RequestBody List<Map<String, Integer>> order) {
        clientInvitationService.reorderSessions(order);
        return ResponseEntity.ok().build();
    }

    // ===================== LOVE STORY =====================

    @GetMapping("/love-stories")
    public ResponseEntity<List<LoveStoryDto>> listLoveStories() {
        return ResponseEntity.ok(clientInvitationService.listLoveStories());
    }

    @GetMapping("/love-stories/{id}")
    public ResponseEntity<LoveStoryDto> getLoveStory(@PathVariable Long id) {
        return ResponseEntity.ok(clientInvitationService.getLoveStory(id));
    }

    @PostMapping("/love-stories")
    public ResponseEntity<LoveStoryDto> createLoveStory(@Valid @RequestBody CreateLoveStoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientInvitationService.createLoveStory(request));
    }

    @PutMapping("/love-stories/{id}")
    public ResponseEntity<LoveStoryDto> updateLoveStory(@PathVariable Long id,
                                                         @RequestBody UpdateLoveStoryRequest request) {
        return ResponseEntity.ok(clientInvitationService.updateLoveStory(id, request));
    }

    @DeleteMapping("/love-stories/{id}")
    public ResponseEntity<Void> deleteLoveStory(@PathVariable Long id) {
        clientInvitationService.deleteLoveStory(id);
        return ResponseEntity.noContent().build();
    }
}
