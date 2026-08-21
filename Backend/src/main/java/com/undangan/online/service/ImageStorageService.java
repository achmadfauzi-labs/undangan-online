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
public class ImageStorageService {

    private final Path imagesRoot;
    private final long maxFileSizeBytes;

    public ImageStorageService(@Qualifier("imagesStoragePath") Path imagesRoot,
                               @Value("${MAX_IMAGE_SIZE_MB:5}") long maxImageSizeMb) {
        this.imagesRoot = imagesRoot;
        this.maxFileSizeBytes = maxImageSizeMb * 1024L * 1024L;
    }

    public String saveGalleryImage(Long clientId, Long invitationId, MultipartFile file) throws IOException {
        validateImage(file);
        String filename = generateFilename(file.getOriginalFilename());
        String relativePath = "images/" + clientId + "/" + invitationId + "/" + filename;
        Path target = imagesRoot.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return relativePath;
    }

    public String savePersonPhoto(Long clientId, Long personId, MultipartFile file) throws IOException {
        validateImage(file);
        String ext = getExtension(file.getOriginalFilename());
        String filename = "person_" + personId + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        String relativePath = "images/" + clientId + "/persons/" + filename;
        Path target = imagesRoot.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return relativePath;
    }

    public void deleteFile(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        Path target = imagesRoot.resolve(relativePath);
        if (Files.exists(target)) {
            Files.delete(target);
        }
    }

    public void replaceFile(String oldRelativePath, String newRelativePath) throws IOException {
        if (oldRelativePath != null && !oldRelativePath.isBlank()) {
            Path oldTarget = imagesRoot.resolve(oldRelativePath);
            if (Files.exists(oldTarget)) {
                Files.delete(oldTarget);
            }
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File gambar wajib diupload");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("Ukuran file maksimal " + (maxFileSizeBytes / 1024 / 1024) + "MB");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.lastIndexOf('.') == -1) {
            throw new IllegalArgumentException("File harus memiliki ekstensi");
        }
        String ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        if (!"jpg".equals(ext) && !"jpeg".equals(ext) && !"png".equals(ext) && !"webp".equals(ext)) {
            throw new IllegalArgumentException("Format file harus jpg, jpeg, png, atau webp");
        }
    }

    private String generateFilename(String originalFilename) {
        String ext = getExtension(originalFilename);
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.lastIndexOf('.') == -1) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
