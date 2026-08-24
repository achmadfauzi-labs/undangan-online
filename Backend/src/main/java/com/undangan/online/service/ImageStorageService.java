package com.undangan.online.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageStorageService {

    String saveGalleryImage(Long clientId, Long invitationId, MultipartFile file) throws IOException;

    String savePersonPhoto(Long clientId, Long personId, MultipartFile file) throws IOException;

    void deleteFile(String relativePath) throws IOException;

    void replaceFile(String oldRelativePath, String newRelativePath) throws IOException;
}
