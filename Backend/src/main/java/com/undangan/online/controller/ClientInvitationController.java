package com.undangan.online.controller;

import com.undangan.online.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<InvitationDto>> getMyInvitation() {
        return ResponseEntity.ok(ApiResponse.ok(clientInvitationService.getMyInvitation()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<InvitationDto>> updateInvitation(@Valid @RequestBody UpdateInvitationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Data undangan berhasil diupdate", clientInvitationService.updateInvitation(request)));
    }

    @PostMapping("/slug/generate")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateSlug(@RequestBody(required = false) Map<String, String> body) {
        String baseSlug = body != null ? body.get("baseSlug") : null;
        return ResponseEntity.ok(ApiResponse.ok(clientInvitationService.generateSlug(baseSlug)));
    }

    @PostMapping("/publish")
    public ResponseEntity<ApiResponse<Void>> publishInvitation() {
        clientInvitationService.publishInvitation();
        return ResponseEntity.ok(ApiResponse.ok("Undangan berhasil dipublikasi", null));
    }

    // ===================== PERSON =====================

    @GetMapping("/persons")
    public ResponseEntity<ApiResponse<List<InvitationPersonDto>>> listPersons() {
        return ResponseEntity.ok(ApiResponse.ok(clientInvitationService.listPersons()));
    }

    @GetMapping("/persons/{id}")
    public ResponseEntity<ApiResponse<InvitationPersonDto>> getPerson(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(clientInvitationService.getPerson(id)));
    }

    @PostMapping("/persons")
    public ResponseEntity<ApiResponse<InvitationPersonDto>> createPerson(@Valid @RequestBody CreateInvitationPersonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(clientInvitationService.createPerson(request)));
    }

    @PutMapping("/persons/{id}")
    public ResponseEntity<ApiResponse<InvitationPersonDto>> updatePerson(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateInvitationPersonRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Person berhasil diupdate", clientInvitationService.updatePerson(id, request)));
    }

    @DeleteMapping("/persons/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePerson(@PathVariable Long id) {
        clientInvitationService.deletePerson(id);
        return ResponseEntity.ok(ApiResponse.ok("Person berhasil dihapus", null));
    }

    // ===================== SESSION =====================

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<InvitationSessionDto>>> listSessions() {
        return ResponseEntity.ok(ApiResponse.ok(clientInvitationService.listSessions()));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<InvitationSessionDto>> getSession(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(clientInvitationService.getSession(id)));
    }

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<InvitationSessionDto>> createSession(@Valid @RequestBody CreateInvitationSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(clientInvitationService.createSession(request)));
    }

    @PutMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<InvitationSessionDto>> updateSession(@PathVariable Long id,
                                                               @Valid @RequestBody UpdateInvitationSessionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Sesi berhasil diupdate", clientInvitationService.updateSession(id, request)));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable Long id) {
        clientInvitationService.deleteSession(id);
        return ResponseEntity.ok(ApiResponse.ok("Sesi berhasil dihapus", null));
    }

    @PatchMapping("/sessions/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderSessions(@RequestBody List<Map<String, Integer>> order) {
        clientInvitationService.reorderSessions(order);
        return ResponseEntity.ok(ApiResponse.ok("Urutan sesi berhasil diupdate", null));
    }

    // ===================== LOVE STORY =====================

    @GetMapping("/love-stories")
    public ResponseEntity<ApiResponse<List<LoveStoryDto>>> listLoveStories() {
        return ResponseEntity.ok(ApiResponse.ok(clientInvitationService.listLoveStories()));
    }

    @GetMapping("/love-stories/{id}")
    public ResponseEntity<ApiResponse<LoveStoryDto>> getLoveStory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(clientInvitationService.getLoveStory(id)));
    }

    @PostMapping("/love-stories")
    public ResponseEntity<ApiResponse<LoveStoryDto>> createLoveStory(@Valid @RequestBody CreateLoveStoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(clientInvitationService.createLoveStory(request)));
    }

    @PutMapping("/love-stories/{id}")
    public ResponseEntity<ApiResponse<LoveStoryDto>> updateLoveStory(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateLoveStoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Love story berhasil diupdate", clientInvitationService.updateLoveStory(id, request)));
    }

    @DeleteMapping("/love-stories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLoveStory(@PathVariable Long id) {
        clientInvitationService.deleteLoveStory(id);
        return ResponseEntity.ok(ApiResponse.ok("Love story berhasil dihapus", null));
    }
}
