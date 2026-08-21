package com.undangan.online.controller;

import com.undangan.online.dto.ClientUploadMusicRequest;
import com.undangan.online.dto.MusicDto;
import com.undangan.online.service.MusicManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/client/musics")
@PreAuthorize("hasRole('USER')")
public class ClientMusicController {

    private final MusicManagementService musicManagementService;

    public ClientMusicController(MusicManagementService musicManagementService) {
        this.musicManagementService = musicManagementService;
    }

    @GetMapping
    public ResponseEntity<List<MusicDto>> listAvailable() {
        Long clientId = getCurrentClientId();
        return ResponseEntity.ok(musicManagementService.listAvailable(clientId));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<MusicDto>> listMine() {
        Long clientId = getCurrentClientId();
        return ResponseEntity.ok(musicManagementService.listMyCustomMusics(clientId));
    }

    @PostMapping("/upload")
    public ResponseEntity<MusicDto> upload(
            @RequestParam("title") String title,
            @RequestParam(value = "artist", required = false) String artist,
            @RequestParam("audioFile") MultipartFile audioFile) throws Exception {
        ClientUploadMusicRequest request = new ClientUploadMusicRequest();
        request.setTitle(title);
        request.setArtist(artist);
        request.setAudioFile(audioFile);
        Long clientId = getCurrentClientId();
        return ResponseEntity.status(HttpStatus.CREATED).body(musicManagementService.uploadCustom(clientId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws Exception {
        Long clientId = getCurrentClientId();
        musicManagementService.deleteCustom(clientId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invitation/music")
    public ResponseEntity<Map<String, String>> assign(
            @RequestBody Map<String, Long> body) {
        Long musicId = body.get("musicId");
        Long clientId = getCurrentClientId();
        musicManagementService.assignToInvitation(clientId, musicId);
        return ResponseEntity.ok(Map.of("success", "true", "message", "Musik berhasil dipilih"));
    }

    @DeleteMapping("/invitation/music")
    public ResponseEntity<Void> remove() {
        Long clientId = getCurrentClientId();
        musicManagementService.removeAssignment(clientId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invitation/music")
    public ResponseEntity<MusicDto> getCurrent() {
        Long clientId = getCurrentClientId();
        MusicDto music = musicManagementService.getCurrentMusic(clientId);
        return ResponseEntity.ok(music);
    }

    private Long getCurrentClientId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new com.undangan.online.exception.AuthException("UNAUTHORIZED", "Tidak terautentikasi", 401);
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof com.undangan.online.entity.Users user) {
            if (user.getClientId() == null) {
                throw new com.undangan.online.exception.AuthException("FORBIDDEN", "User bukan client", 403);
            }
            return user.getClientId();
        }
        throw new com.undangan.online.exception.AuthException("UNAUTHORIZED", "Tidak terautentikasi", 401);
    }
}
