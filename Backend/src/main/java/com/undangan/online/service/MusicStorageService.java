package com.undangan.online.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class MusicStorageService {

    private final Path musicsRoot;
    private final long maxFileSizeBytes;

    public MusicStorageService(@Qualifier("musicsStoragePath") Path musicsRoot,
                               @Value("${MAX_MUSIC_SIZE_MB:10}") long maxFileSizeMb) {
        this.musicsRoot = musicsRoot;
        this.maxFileSizeBytes = maxFileSizeMb * 1024L * 1024L;
    }

    public String saveAdminMusic(MultipartFile file) throws IOException {
        validateFile(file);
        String filename = generateFilename(file.getOriginalFilename());
        Path target = musicsRoot.resolve(filename);
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "musics/" + filename;
    }

    public String saveClientCustomMusic(Long clientId, MultipartFile file) throws IOException {
        validateFile(file);
        String filename = "custom/" + clientId + "/" + generateFilename(file.getOriginalFilename());
        Path target = musicsRoot.resolve(filename);
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "musics/" + filename;
    }

    public void deleteFile(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        Path target = musicsRoot.resolve(relativePath);
        if (Files.exists(target)) {
            Files.delete(target);
        }
    }

    public String replaceFile(String oldRelativePath, MultipartFile newFile) throws IOException {
        validateFile(newFile);
        if (oldRelativePath != null && !oldRelativePath.isBlank()) {
            Path oldTarget = musicsRoot.resolve(oldRelativePath);
            if (Files.exists(oldTarget)) {
                Files.delete(oldTarget);
            }
        }
        String newFilename = generateFilename(newFile.getOriginalFilename());
        Path newTarget = musicsRoot.resolve(newFilename);
        Files.createDirectories(newTarget.getParent());
        Files.copy(newFile.getInputStream(), newTarget, StandardCopyOption.REPLACE_EXISTING);
        return "musics/" + newFilename;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File wajib diupload");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("Ukuran file maksimal " + (maxFileSizeBytes / 1024 / 1024) + "MB");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.lastIndexOf('.') == -1) {
            throw new IllegalArgumentException("File harus memiliki ekstensi");
        }
        String ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        if (!"mp3".equals(ext) && !"wav".equals(ext) && !"ogg".equals(ext)) {
            throw new IllegalArgumentException("Format file harus mp3, wav, atau ogg");
        }
    }

    private String generateFilename(String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.lastIndexOf('.') != -1) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }
}
