package com.undangan.online.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class ThemeStorageService {

    private final Path themesRoot;

    public ThemeStorageService(@Qualifier("themesStoragePath") Path themesRoot) {
        this.themesRoot = themesRoot;
    }

    public String extractZipToThemeFolder(String code, MultipartFile zipFile) throws IOException {
        Path targetDir = themesRoot.resolve(code);
        if (Files.exists(targetDir)) {
            deleteDirectory(targetDir);
        }
        Files.createDirectories(targetDir);

        try (InputStream is = zipFile.getInputStream();
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                if (entryName.contains("..") || entryName.startsWith("/")) {
                    throw new IOException("Path traversal detected in zip entry: " + entryName);
                }
                Path targetFile = targetDir.resolve(entryName);
                Files.createDirectories(targetFile.getParent());
                Files.copy(zis, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        return code + "/";
    }

    public void deleteThemeFolder(String code) throws IOException {
        Path targetDir = themesRoot.resolve(code);
        if (Files.exists(targetDir)) {
            deleteDirectory(targetDir);
        }
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
}
