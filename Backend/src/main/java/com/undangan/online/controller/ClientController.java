package com.undangan.online.controller;

import com.undangan.online.dto.ApiResponse;
import com.undangan.online.dto.ClientDto;
import com.undangan.online.dto.CreateClientRequest;
import com.undangan.online.dto.UpdateClientRequest;
import com.undangan.online.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/client")
@PreAuthorize("hasRole('ADMIN')")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ClientDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok("Berhasil mengambil daftar client",
                clientService.list(status, search, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(clientService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClientDto>> create(@Valid @RequestBody CreateClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Client berhasil dibuat", clientService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientDto>> update(@PathVariable Long id,
                                                @Valid @RequestBody UpdateClientRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Client berhasil diupdate", clientService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ClientDto>> updateStatus(@PathVariable Long id,
                                                       @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.ok("Status client berhasil diupdate", clientService.updateStatus(id, status)));
    }

    @PatchMapping("/{id}/extend")
    public ResponseEntity<ApiResponse<ClientDto>> extendExpiry(@PathVariable Long id,
                                                    @RequestParam Integer additionalMonths) {
        return ResponseEntity.ok(ApiResponse.ok("Expiry client berhasil diperpanjang",
                clientService.extendExpiry(id, additionalMonths)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        clientService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.ok("Client berhasil dihapus", null));
    }
}
