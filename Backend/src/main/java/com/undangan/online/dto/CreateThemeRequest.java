package com.undangan.online.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateThemeRequest {
    @NotBlank(message = "Code wajib diisi")
    @Size(min = 2, max = 30, message = "Code harus 2-30 karakter")
    private String code;

    @NotBlank(message = "Nama wajib diisi")
    @Size(max = 100, message = "Nama maksimal 100 karakter")
    private String name;

    @NotBlank(message = "Category wajib diisi")
    @Size(max = 30, message = "Category maksimal 30 karakter")
    private String category;

    private String thumbnailCss;

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

    public String getThumbnailCss() {
        return thumbnailCss;
    }

    public void setThumbnailCss(String thumbnailCss) {
        this.thumbnailCss = thumbnailCss;
    }
}
