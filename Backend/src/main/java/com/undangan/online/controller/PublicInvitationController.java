package com.undangan.online.controller;

import com.undangan.online.dto.ApiResponse;
import com.undangan.online.service.ClientInvitationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
public class PublicInvitationController {

    private final ClientInvitationService clientInvitationService;

    public PublicInvitationController(ClientInvitationService clientInvitationService) {
        this.clientInvitationService = clientInvitationService;
    }

    @GetMapping("/invitation/{slug}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInvitation(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok("Data undangan berhasil diambil", clientInvitationService.getPublicInvitation(slug)));
    }

    @GetMapping("/invitation/{slug}/guestbook")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getGuestbook(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok("Data guestbook berhasil diambil", clientInvitationService.getPublicGuestbook(slug)));
    }
}
