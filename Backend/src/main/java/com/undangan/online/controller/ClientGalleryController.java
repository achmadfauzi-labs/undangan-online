package com.undangan.online.controller;

import com.undangan.online.dto.CreateGalleryRequest;
import com.undangan.online.dto.GalleryDto;
import com.undangan.online.dto.UpdateGalleryRequest;
import com.undangan.online.service.GalleryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class ClientGalleryController {

    private final GalleryService galleryService;

    public ClientGalleryController(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    // ===================== CLIENT GALLERY =====================

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/api/v1/client/invitation/gallery")
    public ResponseEntity<List<GalleryDto>> listGalleries() {
        return ResponseEntity.ok(galleryService.listGalleries());
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/api/v1/client/invitation/gallery/{id}")
    public ResponseEntity<GalleryDto> getGallery(@PathVariable Long id) {
        return ResponseEntity.ok(galleryService.getGallery(id));
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping(value = "/api/v1/client/invitation/gallery", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GalleryDto> createGallery(
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "sortOrder", required = false) Short sortOrder) throws Exception {
        CreateGalleryRequest request = new CreateGalleryRequest();
        request.setImageFile(imageFile);
        request.setCaption(caption);
        request.setSortOrder(sortOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(galleryService.createGallery(request));
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping(value = "/api/v1/client/invitation/gallery/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GalleryDto> updateGallery(
            @PathVariable Long id,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "sortOrder", required = false) Short sortOrder,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) throws Exception {
        UpdateGalleryRequest request = new UpdateGalleryRequest();
        request.setCaption(caption);
        request.setSortOrder(sortOrder);
        request.setImageFile(imageFile);
        return ResponseEntity.ok(galleryService.updateGallery(id, request));
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/api/v1/client/invitation/gallery/{id}")
    public ResponseEntity<Void> deleteGallery(@PathVariable Long id) throws Exception {
        galleryService.deleteGallery(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/api/v1/client/invitation/gallery/reorder")
    public ResponseEntity<Void> reorderGalleries(@RequestBody List<Map<String, Integer>> order) {
        galleryService.reorderGalleries(order);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping(value = "/api/v1/client/invitation/gallery/bulk", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<GalleryDto>> bulkUpload(
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam(value = "captions", required = false) List<String> captions) throws Exception {
        return ResponseEntity.ok(galleryService.bulkUploadGallery(images, captions));
    }

    // ===================== PERSON PHOTO =====================

    @PreAuthorize("hasRole('USER')")
    @PostMapping(value = "/api/v1/client/invitation/persons/{personId}/photo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadPersonPhoto(
            @PathVariable Long personId,
            @RequestParam("photoFile") MultipartFile photoFile) throws Exception {
        String photoPath = galleryService.uploadPersonPhoto(personId, photoFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", "true", "photoPath", photoPath));
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/api/v1/client/invitation/persons/{personId}/photo")
    public ResponseEntity<Void> deletePersonPhoto(@PathVariable Long personId) throws Exception {
        galleryService.deletePersonPhoto(personId);
        return ResponseEntity.noContent().build();
    }

    // ===================== PUBLIC GALLERY =====================

    @GetMapping("/api/v1/public/invitation/{slug}/gallery")
    public ResponseEntity<List<Map<String, String>>> getPublicGallery(@PathVariable String slug) {
        return ResponseEntity.ok(galleryService.getPublicGallery(slug));
    }
}
