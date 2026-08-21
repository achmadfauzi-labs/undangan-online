package com.undangan.online.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateLoveStoryRequest {
    @NotBlank(message = "Judul wajib diisi")
    @Size(max = 150, message = "Judul maksimal 150 karakter")
    private String title;

    private java.time.LocalDate storyDate;

    @NotBlank(message = "Deskripsi wajib diisi")
    private String description;

    @NotNull(message = "Sort order wajib diisi")
    private Short sortOrder;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public java.time.LocalDate getStoryDate() {
        return storyDate;
    }

    public void setStoryDate(java.time.LocalDate storyDate) {
        this.storyDate = storyDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Short getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Short sortOrder) {
        this.sortOrder = sortOrder;
    }
}
