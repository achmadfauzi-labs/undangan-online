package com.undangan.online.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ThemeStorageService {

    String extractZipToThemeFolder(String code, MultipartFile zipFile) throws IOException;

    void deleteThemeFolder(String code) throws IOException;
}
