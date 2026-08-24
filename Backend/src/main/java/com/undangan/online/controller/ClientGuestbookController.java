package com.undangan.online.controller;

import com.undangan.online.dto.ApiResponse;
import com.undangan.online.dto.GuestbookEntryDto;
import com.undangan.online.dto.GuestbookModerationDto;
import com.undangan.online.service.ClientGuestbookService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/client/guestbook")
@PreAuthorize("hasRole('USER')")
public class ClientGuestbookController {

    private final ClientGuestbookService clientGuestbookService;

    public ClientGuestbookController(ClientGuestbookService clientGuestbookService) {
        this.clientGuestbookService = clientGuestbookService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GuestbookEntryDto>>> list(
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok("Berhasil mengambil data", clientGuestbookService.listEntries(isPublished, search)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GuestbookEntryDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(clientGuestbookService.getEntry(id)));
    }

    @PatchMapping("/{id}/reply")
    public ResponseEntity<ApiResponse<GuestbookEntryDto>> reply(@PathVariable Long id,
                                                               @Valid @RequestBody GuestbookModerationDto request) {
        return ResponseEntity.ok(ApiResponse.ok("Balasan berhasil disimpan", clientGuestbookService.replyToEntry(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        clientGuestbookService.deleteEntry(id);
        return ResponseEntity.ok(ApiResponse.ok("Guestbook entry berhasil dihapus", null));
    }
}
