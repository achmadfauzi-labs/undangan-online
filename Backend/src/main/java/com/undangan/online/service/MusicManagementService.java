package com.undangan.online.service;

import com.undangan.online.dto.ClientUploadMusicRequest;
import com.undangan.online.dto.CreateMusicRequest;
import com.undangan.online.dto.MusicDto;
import com.undangan.online.entity.Invitation;
import com.undangan.online.entity.Music;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.InvitationRepository;
import com.undangan.online.repository.MusicRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MusicManagementService {

    private final MusicRepository musicRepository;
    private final InvitationRepository invitationRepository;
    private final MusicStorageService musicStorageService;

    public MusicManagementService(MusicRepository musicRepository,
                                  InvitationRepository invitationRepository,
                                  MusicStorageService musicStorageService) {
        this.musicRepository = musicRepository;
        this.invitationRepository = invitationRepository;
        this.musicStorageService = musicStorageService;
    }

    // ===================== ADMIN =====================

    @Transactional(readOnly = true)
    public List<MusicDto> listAll(String status, String search) {
        List<Music> musics = musicRepository.findAll();
        return musics.stream()
                .filter(m -> status == null || status.isBlank() || status.equals(m.getStatus()))
                .filter(m -> search == null || search.isBlank() ||
                        (m.getTitle() != null && m.getTitle().toLowerCase().contains(search.toLowerCase())) ||
                        (m.getArtist() != null && m.getArtist().toLowerCase().contains(search.toLowerCase())))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MusicDto getById(Long id) {
        Music music = musicRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Music tidak ditemukan"));
        return toDto(music);
    }

    @Transactional
    public MusicDto create(CreateMusicRequest request) throws IOException {
        if (request.getAudioFile() == null || request.getAudioFile().isEmpty()) {
            throw new AuthException("VALIDATION_ERROR", "File audio wajib diupload", 400);
        }

        String filePath = musicStorageService.saveAdminMusic(request.getAudioFile());

        Music music = new Music();
        music.setTitle(request.getTitle());
        music.setArtist(request.getArtist());
        music.setFilePath(filePath);
        music.setFileSize((int) request.getAudioFile().getSize());
        music.setStatus("active");
        music.setCreatedAt(OffsetDateTime.now());
        music.setUpdatedAt(OffsetDateTime.now());

        musicRepository.save(music);
        return toDto(music);
    }

    @Transactional
    public MusicDto update(Long id, CreateMusicRequest request) {
        Music music = musicRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Music tidak ditemukan"));

        if (request.getTitle() != null) {
            music.setTitle(request.getTitle());
        }
        if (request.getArtist() != null) {
            music.setArtist(request.getArtist());
        }
        music.setUpdatedAt(OffsetDateTime.now());
        musicRepository.save(music);
        return toDto(music);
    }

    @Transactional
    public MusicDto uploadFile(Long id, MultipartFile audioFile) throws IOException {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new AuthException("VALIDATION_ERROR", "File audio wajib diupload", 400);
        }

        Music music = musicRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Music tidak ditemukan"));

        String oldPath = music.getFilePath();
        String newPath = musicStorageService.replaceFile(oldPath, audioFile);
        music.setFilePath(newPath);
        music.setFileSize((int) audioFile.getSize());
        music.setUpdatedAt(OffsetDateTime.now());
        musicRepository.save(music);
        return toDto(music);
    }

    @Transactional
    public MusicDto updateStatus(Long id, String status) {
        if (!"active".equals(status) && !"inactive".equals(status)) {
            throw new AuthException("VALIDATION_ERROR", "Status harus 'active' atau 'inactive'", 400);
        }

        Music music = musicRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Music tidak ditemukan"));

        music.setStatus(status);
        music.setUpdatedAt(OffsetDateTime.now());
        musicRepository.save(music);
        return toDto(music);
    }

    @Transactional
    public void delete(Long id) throws IOException {
        Music music = musicRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Music tidak ditemukan"));

        musicStorageService.deleteFile(music.getFilePath());
        musicRepository.delete(music);
    }

    // ===================== CLIENT =====================

    @Transactional(readOnly = true)
    public List<MusicDto> listAvailable(Long clientId) {
        List<Music> adminMusics = musicRepository.findByStatus("active");
        List<Music> allMusics = new ArrayList<>(adminMusics);

        List<Music> customMusics = musicRepository.findAll().stream()
                .filter(m -> m.getFilePath() != null && m.getFilePath().startsWith("musics/custom/" + clientId + "/"))
                .filter(m -> "active".equals(m.getStatus()))
                .collect(Collectors.toList());
        allMusics.addAll(customMusics);

        return allMusics.stream()
                .map(m -> {
                    MusicDto dto = toDto(m);
                    dto.setIsCustom(m.getFilePath() != null && m.getFilePath().startsWith("musics/custom/" + clientId + "/"));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MusicDto> listMyCustomMusics(Long clientId) {
        return musicRepository.findAll().stream()
                .filter(m -> m.getFilePath() != null && m.getFilePath().startsWith("musics/custom/" + clientId + "/"))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MusicDto uploadCustom(Long clientId, ClientUploadMusicRequest request) throws IOException {
        if (request.getAudioFile() == null || request.getAudioFile().isEmpty()) {
            throw new AuthException("VALIDATION_ERROR", "File audio wajib diupload", 400);
        }

        String filePath = musicStorageService.saveClientCustomMusic(clientId, request.getAudioFile());

        Music music = new Music();
        music.setTitle(request.getTitle());
        music.setArtist(request.getArtist());
        music.setFilePath(filePath);
        music.setFileSize((int) request.getAudioFile().getSize());
        music.setStatus("active");
        music.setCreatedAt(OffsetDateTime.now());
        music.setUpdatedAt(OffsetDateTime.now());

        musicRepository.save(music);
        return toDto(music);
    }

    @Transactional
    public void deleteCustom(Long clientId, Long musicId) throws IOException {
        Music music = musicRepository.findById(musicId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Music tidak ditemukan"));

        if (music.getFilePath() == null || !music.getFilePath().startsWith("musics/custom/" + clientId + "/")) {
            throw new AuthException("FORBIDDEN", "Bukan musik custom milik Anda", 403);
        }

        musicStorageService.deleteFile(music.getFilePath());
        musicRepository.delete(music);
    }

    @Transactional
    public void assignToInvitation(Long clientId, Long musicId) {
        Invitation invitation = invitationRepository.findByClientId(clientId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Invitation belum dibuat"));

        Music music = musicRepository.findById(musicId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Music tidak ditemukan"));

        if (!"active".equals(music.getStatus())) {
            throw new AuthException("VALIDATION_ERROR", "Music tidak aktif", 400);
        }

        invitation.setPrimaryMusicId(musicId);
        invitation.setCustomMusicPath(null);
        invitation.setCustomMusicTitle(null);
        invitation.setUpdatedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);
    }

    @Transactional
    public void removeAssignment(Long clientId) {
        Invitation invitation = invitationRepository.findByClientId(clientId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Invitation belum dibuat"));

        invitation.setPrimaryMusicId(null);
        invitation.setCustomMusicPath(null);
        invitation.setCustomMusicTitle(null);
        invitation.setUpdatedAt(OffsetDateTime.now());
        invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public MusicDto getCurrentMusic(Long clientId) {
        Invitation invitation = invitationRepository.findByClientId(clientId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Invitation belum dibuat"));

        if (invitation.getPrimaryMusicId() != null) {
            return getById(invitation.getPrimaryMusicId());
        }

        if (invitation.getCustomMusicPath() != null) {
            Music custom = musicRepository.findAll().stream()
                    .filter(m -> invitation.getCustomMusicPath().equals(m.getFilePath()))
                    .findFirst()
                    .orElse(null);
            if (custom != null) {
                MusicDto dto = toDto(custom);
                dto.setIsCustom(true);
                return dto;
            }
        }

        return null;
    }

    private MusicDto toDto(Music music) {
        MusicDto dto = new MusicDto();
        dto.setId(music.getId());
        dto.setTitle(music.getTitle());
        dto.setArtist(music.getArtist());
        dto.setFilePath(music.getFilePath());
        dto.setFileSize(music.getFileSize());
        dto.setStatus(music.getStatus());
        dto.setCreatedAt(music.getCreatedAt());
        dto.setUpdatedAt(music.getUpdatedAt());
        return dto;
    }
}
