package com.undangan.online.service.impl;

import com.undangan.online.config.StorageConfig;
import com.undangan.online.service.ThemeStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ThemeStorageServiceImpl implements ThemeStorageService {

    private static final Logger log = LoggerFactory.getLogger(ThemeStorageServiceImpl.class);

    private final Path themesRoot;
    private final StorageConfig storageConfig;

    public ThemeStorageServiceImpl(@Qualifier("themesStoragePath") Path themesRoot,
                                   StorageConfig storageConfig) {
        this.themesRoot = themesRoot;
        this.storageConfig = storageConfig;
    }

    @Override
    public String extractZipToThemeFolder(String code, MultipartFile zipFile) throws IOException {
        String originalName = zipFile.getOriginalFilename() != null ? zipFile.getOriginalFilename() : "theme.zip";
        String actorUsername = getCurrentUsername();
        log.info("Theme file uploaded: name='{}', size={} bytes, by admin {}", originalName, zipFile.getSize(), actorUsername);

        validateZipFile(zipFile);

        String safeCode = sanitizeCode(code);
        Path targetDir = themesRoot.resolve(safeCode);
        if (Files.exists(targetDir)) {
            deleteDirectory(targetDir);
        }

        Path tmpDir = Files.createTempDirectory(themesRoot, "tmp_" + safeCode + "_");
        try {
            Files.createDirectories(tmpDir);
            extractEntries(zipFile, tmpDir);
            Files.move(tmpDir, targetDir, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            try {
                deleteDirectory(tmpDir);
            } catch (IOException ignored) {
            }
            log.error("Theme upload failed: name='{}', code='{}'", originalName, safeCode, ex);
            throw ex;
        }

        log.info("Theme extracted to folder: code='{}'", safeCode);
        return safeCode + "/";
    }

    @Override
    public void deleteThemeFolder(String code) throws IOException {
        String safeCode = sanitizeCode(code);
        Path targetDir = themesRoot.resolve(safeCode);
        if (Files.exists(targetDir)) {
            deleteDirectory(targetDir);
            log.warn("Theme file deleted: path='{}', by admin {}", safeCode + "/", getCurrentUsername());
        } else {
            log.warn("Theme file tidak ditemukan untuk dihapus: code='{}'", safeCode);
        }
    }

    private void validateZipFile(MultipartFile zipFile) throws IOException {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new IOException("File tema wajib diupload");
        }
        String original = zipFile.getOriginalFilename();
        if (original == null || !original.toLowerCase().endsWith(".zip")) {
            throw new IOException("File tema harus berupa .zip");
        }
        long maxSize = storageConfig.getMaxMusicSizeMb() * 1024L * 1024L;
        if (zipFile.getSize() > maxSize) {
            throw new IOException("File tema melebihi batas maksimum " + storageConfig.getMaxMusicSizeMb() + "MB");
        }
    }

    private void extractEntries(MultipartFile zipFile, Path targetDir) throws IOException {
        try (InputStream is = zipFile.getInputStream();
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                    throw new IOException("Path traversal terdeteksi pada zip entry: " + entryName);
                }
                Path targetFile = targetDir.resolve(entryName);
                Path normalized = targetFile.normalize();
                if (!normalized.startsWith(targetDir.normalize())) {
                    throw new IOException("Path traversal terdeteksi pada zip entry: " + entryName);
                }
                Files.createDirectories(targetFile.getParent());
                Files.copy(zis, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private String sanitizeCode(String code) {
        if (code == null || code.isBlank()) {
            return "theme";
        }
        String cleaned = code.replaceAll("[^a-zA-Z0-9-_]", "_");
        if (cleaned.contains("..")) {
            cleaned = "theme";
        }
        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64);
        }
        return cleaned;
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                stream.forEach(child -> {
                    try {
                        deleteDirectory(child);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        Files.deleteIfExists(path);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails ud) {
                return ud.getUsername();
            }
            if (principal instanceof String s && !s.isBlank()) {
                return s;
            }
            return auth.getName();
        }
        return "unknown";
    }
}
