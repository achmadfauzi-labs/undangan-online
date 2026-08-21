package com.undangan.online.controller;

import com.undangan.online.dto.CreateGuestRequest;
import com.undangan.online.dto.GuestDto;
import com.undangan.online.dto.UpdateGuestRequest;
import com.undangan.online.service.ClientGuestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/client/guests")
@PreAuthorize("hasRole('USER')")
public class ClientGuestController {

    private final ClientGuestService clientGuestService;

    public ClientGuestController(ClientGuestService clientGuestService) {
        this.clientGuestService = clientGuestService;
    }

    @GetMapping
    public ResponseEntity<List<GuestDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(clientGuestService.listGuests(status, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuestDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clientGuestService.getGuest(id));
    }

    @PostMapping
    public ResponseEntity<GuestDto> create(@Valid @RequestBody CreateGuestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientGuestService.createGuest(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuestDto> update(@PathVariable Long id,
                                           @RequestBody UpdateGuestRequest request) {
        return ResponseEntity.ok(clientGuestService.updateGuest(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientGuestService.deleteGuest(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<GuestDto>> bulkCreate(@RequestBody List<CreateGuestRequest> requests) {
        return ResponseEntity.ok(clientGuestService.bulkCreateGuests(requests));
    }

    @GetMapping("/{id}/link")
    public ResponseEntity<Map<String, Object>> generateLink(@PathVariable Long id) {
        return ResponseEntity.ok(clientGuestService.generateGuestLink(id));
    }

    @GetMapping("/export")
    public ResponseEntity<List<GuestDto>> export() {
        return ResponseEntity.ok(clientGuestService.exportGuests());
    }
}
