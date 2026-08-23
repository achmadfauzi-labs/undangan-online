package com.undangan.online.controller;

import com.undangan.online.dto.ApiResponse;
import com.undangan.online.dto.CreateGuestRequest;
import com.undangan.online.dto.GuestDto;
import com.undangan.online.dto.UpdateGuestRequest;
import com.undangan.online.service.ClientGuestService;
import jakarta.validation.Valid;
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
    public ResponseEntity<ApiResponse<List<GuestDto>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok("Berhasil mengambil data", clientGuestService.listGuests(status, search)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GuestDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(clientGuestService.getGuest(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GuestDto>> create(@Valid @RequestBody CreateGuestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(clientGuestService.createGuest(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GuestDto>> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateGuestRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Guest berhasil diupdate", clientGuestService.updateGuest(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        clientGuestService.deleteGuest(id);
        return ResponseEntity.ok(ApiResponse.ok("Guest berhasil dihapus", null));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<GuestDto>>> bulkCreate(@RequestBody List<CreateGuestRequest> requests) {
        return ResponseEntity.ok(ApiResponse.ok("Bulk guest berhasil dibuat", clientGuestService.bulkCreateGuests(requests)));
    }

    @GetMapping("/{id}/link")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateLink(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(clientGuestService.generateGuestLink(id)));
    }

    @GetMapping("/export")
    public ResponseEntity<ApiResponse<List<GuestDto>>> export() {
        return ResponseEntity.ok(ApiResponse.ok(clientGuestService.exportGuests()));
    }
}
