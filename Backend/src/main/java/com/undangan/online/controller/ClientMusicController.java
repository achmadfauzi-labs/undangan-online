package com.undangan.online.controller;

import com.undangan.online.dto.ApiResponse;
import com.undangan.online.dto.ClientUploadMusicRequest;
import com.undangan.online.dto.MusicDto;
import com.undangan.online.entity.Users;
import com.undangan.online.exception.AuthException;
import com.undangan.online.service.MusicManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/client/musics")
@PreAuthorize("hasRole('USER')")
public class ClientMusicController {

    private final MusicManagementService musicManagementService;

    public ClientMusicController(MusicManagementService musicManagementService) {
        this.musicManagementService = musicManagementService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MusicDto>>> listAvailable() {
        Long clientId = getCurrentClientId();
        return ResponseEntity.ok(ApiResponse.ok(musicManagementService.listAvailable(clientId)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<MusicDto>>> listMine() {
        Long clientId = getCurrentClientId();
        return ResponseEntity.ok(ApiResponse.ok(musicManagementService.listMyCustomMusics(clientId)));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<MusicDto>> upload(
            @RequestParam("title") String title,
            @RequestParam(value = "artist", required = false) String artist,
            @RequestParam("audioFile") MultipartFile audioFile) throws Exception {
        ClientUploadMusicRequest request = new ClientUploadMusicRequest();
        request.setTitle(title);
        request.setArtist(artist);
        request.setAudioFile(audioFile);
        Long clientId = getCurrentClientId();
        MusicDto created = musicManagementService.uploadCustom(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Musik berhasil diupload", created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) throws Exception {
        Long clientId = getCurrentClientId();
        musicManagementService.deleteCustom(clientId, id);
        return ResponseEntity.ok(ApiResponse.ok("Musik berhasil dihapus", null));
    }

    @PostMapping("/invitation/music")
    public ResponseEntity<ApiResponse<Void>> assign(
            @RequestBody java.util.Map<String, Long> body) {
        Long musicId = body.get("musicId");
        Long clientId = getCurrentClientId();
        musicManagementService.assignToInvitation(clientId, musicId);
        return ResponseEntity.ok(ApiResponse.ok("Musik berhasil dipilih", null));
    }

    @DeleteMapping("/invitation/music")
    public ResponseEntity<ApiResponse<Void>> remove() {
        Long clientId = getCurrentClientId();
        musicManagementService.removeAssignment(clientId);
        return ResponseEntity.ok(ApiResponse.ok("Musik berhasil dihapus dari undangan", null));
    }

    @GetMapping("/invitation/music")
    public ResponseEntity<ApiResponse<MusicDto>> getCurrent() {
        Long clientId = getCurrentClientId();
        MusicDto music = musicManagementService.getCurrentMusic(clientId);
        return ResponseEntity.ok(ApiResponse.ok(music));
    }

    private Long getCurrentClientId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthException("UNAUTHORIZED", "Tidak terautentikasi", 401);
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Users user) {
            if (user.getClientId() == null) {
                throw new AuthException("FORBIDDEN", "User bukan client", 403);
            }
            return user.getClientId();
        }
        throw new AuthException("UNAUTHORIZED", "Tidak terautentikasi", 401);
    }
}
