package com.undangan.online.controller;

import com.undangan.online.dto.ApiResponse;
import com.undangan.online.dto.RsvpSubmitRequest;
import com.undangan.online.service.PublicRsvpService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
public class PublicRsvpController {

    private final PublicRsvpService publicRsvpService;

    public PublicRsvpController(PublicRsvpService publicRsvpService) {
        this.publicRsvpService = publicRsvpService;
    }

    @PostMapping("/rsvp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitRsvp(@Valid @RequestBody RsvpSubmitRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("RSVP berhasil tercatat", publicRsvpService.submitRsvp(request)));
    }

    @GetMapping("/rsvp/{token}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRsvpStatus(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(publicRsvpService.getRsvpStatus(token)));
    }

    @PostMapping("/guestbook")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitGuestbook(@RequestBody Map<String, String> body) {
        String slug = body != null ? body.get("slug") : null;
        String name = body != null ? body.get("name") : null;
        String message = body != null ? body.get("message") : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Ucapan berhasil dikirim", publicRsvpService.submitGuestbook(slug, name, message)));
    }

    @GetMapping("/guestbook/{slug}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getGuestbook(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok("Daftar ucapan berhasil diambil", publicRsvpService.getGuestbook(slug)));
    }
}
