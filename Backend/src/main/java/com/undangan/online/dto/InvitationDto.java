package com.undangan.online.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class InvitationDto {
    private Long id;
    private Long clientId;
    private String slug;
    private String eventTypeCode;
    private String status;
    private String welcomeMessage;
    private String coverImagePath;
    private Long templateId;
    private Long primaryMusicId;
    private String customMusicPath;
    private String customMusicTitle;
    private OffsetDateTime publishedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<InvitationPersonDto> persons;
    private List<InvitationSessionDto> sessions;
    private List<LoveStoryDto> loveStories;

    public InvitationDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getEventTypeCode() {
        return eventTypeCode;
    }

    public void setEventTypeCode(String eventTypeCode) {
        this.eventTypeCode = eventTypeCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public String getCoverImagePath() {
        return coverImagePath;
    }

    public void setCoverImagePath(String coverImagePath) {
        this.coverImagePath = coverImagePath;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getPrimaryMusicId() {
        return primaryMusicId;
    }

    public void setPrimaryMusicId(Long primaryMusicId) {
        this.primaryMusicId = primaryMusicId;
    }

    public String getCustomMusicPath() {
        return customMusicPath;
    }

    public void setCustomMusicPath(String customMusicPath) {
        this.customMusicPath = customMusicPath;
    }

    public String getCustomMusicTitle() {
        return customMusicTitle;
    }

    public void setCustomMusicTitle(String customMusicTitle) {
        this.customMusicTitle = customMusicTitle;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
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

    public List<InvitationPersonDto> getPersons() {
        return persons;
    }

    public void setPersons(List<InvitationPersonDto> persons) {
        this.persons = persons;
    }

    public List<InvitationSessionDto> getSessions() {
        return sessions;
    }

    public void setSessions(List<InvitationSessionDto> sessions) {
        this.sessions = sessions;
    }

    public List<LoveStoryDto> getLoveStories() {
        return loveStories;
    }

    public void setLoveStories(List<LoveStoryDto> loveStories) {
        this.loveStories = loveStories;
    }
}
