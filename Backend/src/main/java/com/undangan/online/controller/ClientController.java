package com.undangan.online.controller;

import com.undangan.online.dto.ClientDto;
import com.undangan.online.dto.CreateClientRequest;
import com.undangan.online.dto.UpdateClientRequest;
import com.undangan.online.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/client")
@PreAuthorize("hasRole('ADMIN')")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public ResponseEntity<Page<ClientDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        return ResponseEntity.ok(clientService.list(status, search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ClientDto> create(@Valid @RequestBody CreateClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDto> update(@PathVariable Long id,
                                            @Valid @RequestBody UpdateClientRequest request) {
        return ResponseEntity.ok(clientService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ClientDto> updateStatus(@PathVariable Long id,
                                                  @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(clientService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/extend")
    public ResponseEntity<ClientDto> extendExpiry(@PathVariable Long id,
                                                  @RequestBody Map<String, Object> body) {
        Integer additionalMonths = null;
        if (body.get("additionalMonths") instanceof Number) {
            additionalMonths = ((Number) body.get("additionalMonths")).intValue();
        }
        return ResponseEntity.ok(clientService.extendExpiry(id, additionalMonths));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
