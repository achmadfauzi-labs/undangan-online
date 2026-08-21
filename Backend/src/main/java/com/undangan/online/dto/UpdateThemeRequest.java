package com.undangan.online.dto;

import jakarta.validation.constraints.Size;

public class UpdateThemeRequest {
    @Size(max = 100, message = "Nama maksimal 100 karakter")
    private String name;

    @Size(max = 30, message = "Category maksimal 30 karakter")
    private String category;

    private String thumbnailCss;

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

    public String getThumbnailCss() {
        return thumbnailCss;
    }

    public void setThumbnailCss(String thumbnailCss) {
        this.thumbnailCss = thumbnailCss;
    }
}
