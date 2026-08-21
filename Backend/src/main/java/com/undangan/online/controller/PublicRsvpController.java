package com.undangan.online.controller;

import com.undangan.online.dto.RsvpSubmitRequest;
import com.undangan.online.dto.RsvpUpdateRequest;
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
    public ResponseEntity<Map<String, Object>> submitRsvp(@Valid @RequestBody RsvpSubmitRequest request) {
        return ResponseEntity.ok(publicRsvpService.submitRsvp(request));
    }

    @GetMapping("/rsvp/{token}")
    public ResponseEntity<Map<String, Object>> getRsvpStatus(@PathVariable String token) {
        return ResponseEntity.ok(publicRsvpService.getRsvpStatus(token));
    }

    @PostMapping("/guestbook")
    public ResponseEntity<Map<String, Object>> submitGuestbook(@RequestBody Map<String, String> body) {
        String slug = body.get("slug");
        String name = body.get("name");
        String message = body.get("message");
        return ResponseEntity.status(HttpStatus.CREATED).body(publicRsvpService.submitGuestbook(slug, name, message));
    }

    @GetMapping("/guestbook/{slug}")
    public ResponseEntity<List<Map<String, Object>>> getGuestbook(@PathVariable String slug) {
        return ResponseEntity.ok(publicRsvpService.getGuestbook(slug));
    }
}
