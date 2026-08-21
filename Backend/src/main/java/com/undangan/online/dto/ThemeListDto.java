package com.undangan.online.dto;

import java.time.OffsetDateTime;

public class ThemeListDto {
    private Long id;
    private String code;
    private String name;
    private String category;
    private String folderPath;
    private String thumbnailCss;
    private String status;
    private OffsetDateTime createdAt;

    public ThemeListDto() {}

    public ThemeListDto(Long id, String code, String name, String category, String folderPath, String thumbnailCss, String status, OffsetDateTime createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.category = category;
        this.folderPath = folderPath;
        this.thumbnailCss = thumbnailCss;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getThumbnailCss() {
        return thumbnailCss;
    }

    public void setThumbnailCss(String thumbnailCss) {
        this.thumbnailCss = thumbnailCss;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
