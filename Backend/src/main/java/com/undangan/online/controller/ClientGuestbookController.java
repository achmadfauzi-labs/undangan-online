package com.undangan.online.controller;

import com.undangan.online.dto.GuestbookModerationDto;
import com.undangan.online.dto.GuestbookEntryDto;
import com.undangan.online.service.ClientGuestbookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/client/guestbook")
@PreAuthorize("hasRole('USER')")
public class ClientGuestbookController {

    private final ClientGuestbookService clientGuestbookService;

    public ClientGuestbookController(ClientGuestbookService clientGuestbookService) {
        this.clientGuestbookService = clientGuestbookService;
    }

    @GetMapping
    public ResponseEntity<List<GuestbookEntryDto>> list(
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(clientGuestbookService.listEntries(isPublished, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuestbookEntryDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clientGuestbookService.getEntry(id));
    }

    @PatchMapping("/{id}/reply")
    public ResponseEntity<GuestbookEntryDto> reply(@PathVariable Long id,
                                                   @Valid @RequestBody GuestbookModerationDto request) {
        return ResponseEntity.ok(clientGuestbookService.replyToEntry(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientGuestbookService.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }
}
