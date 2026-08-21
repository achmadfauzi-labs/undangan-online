package com.undangan.online.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdateLoveStoryRequest {
    @Size(max = 150, message = "Judul maksimal 150 karakter")
    private String title;

    private LocalDate storyDate;

    private String description;

    private Short sortOrder;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getStoryDate() {
        return storyDate;
    }

    public void setStoryDate(LocalDate storyDate) {
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
