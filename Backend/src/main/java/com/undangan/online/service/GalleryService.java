package com.undangan.online.service;

import com.undangan.online.dto.CreateGalleryRequest;
import com.undangan.online.dto.GalleryDto;
import com.undangan.online.dto.UpdateGalleryRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface GalleryService {

    List<GalleryDto> listGalleries();

    GalleryDto getGallery(Long galleryId);

    GalleryDto createGallery(CreateGalleryRequest request) throws IOException;

    GalleryDto updateGallery(Long galleryId, UpdateGalleryRequest request) throws IOException;

    void deleteGallery(Long galleryId) throws IOException;

    void reorderGalleries(List<Map<String, Integer>> order);

    List<GalleryDto> bulkUploadGallery(List<MultipartFile> images, List<String> captions) throws IOException;

    String uploadPersonPhoto(Long personId, MultipartFile photoFile) throws IOException;

    void deletePersonPhoto(Long personId) throws IOException;

    List<Map<String, String>> getPublicGallery(String slug);
}
