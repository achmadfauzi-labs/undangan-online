package com.undangan.online.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class ClientUploadMusicRequest {
    @NotBlank(message = "Title wajib diisi")
    @Size(max = 150, message = "Title maksimal 150 karakter")
    private String title;

    @Size(max = 150, message = "Artist maksimal 150 karakter")
    private String artist;

    private MultipartFile audioFile;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public MultipartFile getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(MultipartFile audioFile) {
        this.audioFile = audioFile;
    }
}
