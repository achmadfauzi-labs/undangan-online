package com.undangan.online.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class LoveStoryDto {
    private Long id;
    private Long invitationId;
    private String title;
    private LocalDate storyDate;
    private String description;
    private Short sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public LoveStoryDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(Long invitationId) {
        this.invitationId = invitationId;
    }

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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
