package com.undangan.online.service;

import com.undangan.online.entity.Users;
import com.undangan.online.exception.AuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ImageStorageServiceImpl implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(ImageStorageServiceImpl.class);

    private final Path imagesRoot;
    private final long maxFileSizeBytes;

    public ImageStorageServiceImpl(@Qualifier("imagesStoragePath") Path imagesRoot,
                                   @Value("${MAX_IMAGE_SIZE_MB:5}") long maxImageSizeMb) {
        this.imagesRoot = imagesRoot;
        this.maxFileSizeBytes = maxImageSizeMb * 1024L * 1024L;
    }

    @Override
    public String saveGalleryImage(Long clientId, Long invitationId, MultipartFile file) throws IOException {
        validateImage(file);
        String filename = generateFilename(file.getOriginalFilename());
        String relativePath = "images/" + clientId + "/" + invitationId + "/" + filename;
        Path target = imagesRoot.resolve(relativePath);
        Path temp = imagesRoot.resolve(relativePath + ".tmp");
        Files.createDirectories(target.getParent());
        String actor = getCurrentUsername();
        try {
            Files.copy(file.getInputStream(), temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Gallery image uploaded: originalName='{}', savedAs='{}', size={} bytes, invitationId={}, clientId={}, by {}",
                    file.getOriginalFilename(), filename, file.getSize(), invitationId, clientId, actor);
            return relativePath;
        } finally {
            if (Files.exists(temp)) {
                Files.deleteIfExists(temp);
            }
        }
    }

    @Override
    public String savePersonPhoto(Long clientId, Long personId, MultipartFile file) throws IOException {
        validateImage(file);
        String ext = getExtension(file.getOriginalFilename());
        String filename = "person_" + personId + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        String relativePath = "images/" + clientId + "/persons/" + filename;
        Path target = imagesRoot.resolve(relativePath);
        Path temp = imagesRoot.resolve(relativePath + ".tmp");
        Files.createDirectories(target.getParent());
        String actor = getCurrentUsername();
        try {
            Files.copy(file.getInputStream(), temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Gallery image uploaded: originalName='{}', savedAs='{}', size={} bytes, personId={}, clientId={}, by {}",
                    file.getOriginalFilename(), filename, file.getSize(), personId, clientId, actor);
            return relativePath;
        } finally {
            if (Files.exists(temp)) {
                Files.deleteIfExists(temp);
            }
        }
    }

    @Override
    public void deleteFile(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        Path target = imagesRoot.resolve(relativePath);
        if (Files.exists(target)) {
            Files.delete(target);
            log.warn("Gallery image deleted: path='{}', by clientId={}", relativePath, getCurrentUsername());
        }
    }

    @Override
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
            throw new AuthException("VALIDATION_ERROR", "File gambar wajib diupload", 400);
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new AuthException("VALIDATION_ERROR", "Ukuran file maksimal " + (maxFileSizeBytes / 1024 / 1024) + "MB", 400);
        }
        String original = file.getOriginalFilename();
        if (original == null || original.lastIndexOf('.') == -1) {
            throw new AuthException("VALIDATION_ERROR", "File harus memiliki ekstensi", 400);
        }
        String ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        if (!"jpg".equals(ext) && !"jpeg".equals(ext) && !"png".equals(ext) && !"gif".equals(ext) && !"webp".equals(ext)) {
            throw new AuthException("VALIDATION_ERROR", "Format file harus jpg, jpeg, png, gif, atau webp", 400);
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

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Users user) {
            return user.getUsername();
        }
        return "unknown";
    }
}
