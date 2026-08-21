package com.undangan.online.controller;

import com.undangan.online.dto.CreateMusicRequest;
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
@RequestMapping("/api/v1/admin/musics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMusicController {

    private final MusicManagementService musicManagementService;

    public AdminMusicController(MusicManagementService musicManagementService) {
        this.musicManagementService = musicManagementService;
    }

    @GetMapping
    public ResponseEntity<List<MusicDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(musicManagementService.listAll(status, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MusicDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(musicManagementService.getById(id));
    }

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MusicDto> create(
            @RequestParam("title") String title,
            @RequestParam(value = "artist", required = false) String artist,
            @RequestParam("audioFile") MultipartFile audioFile) throws Exception {
        CreateMusicRequest request = new CreateMusicRequest();
        request.setTitle(title);
        request.setArtist(artist);
        request.setAudioFile(audioFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(musicManagementService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MusicDto> update(@PathVariable Long id,
                                           @Valid @RequestBody CreateMusicRequest request) {
        return ResponseEntity.ok(musicManagementService.update(id, request));
    }

    @PatchMapping(value = "/{id}/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MusicDto> uploadFile(@PathVariable Long id,
                                                @RequestParam("audioFile") MultipartFile audioFile) throws Exception {
        return ResponseEntity.ok(musicManagementService.uploadFile(id, audioFile));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<MusicDto> updateStatus(@PathVariable Long id,
                                                  @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(musicManagementService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws Exception {
        musicManagementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
