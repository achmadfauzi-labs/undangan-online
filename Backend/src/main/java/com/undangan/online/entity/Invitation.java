package com.undangan.online.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invitation")
public class Invitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "slug", unique = true, nullable = false, length = 100)
    private String slug;

    @Column(name = "event_type_code", nullable = false, length = 50)
    private String eventTypeCode;

    @Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private String status;

    @Column(name = "welcome_message")
    private String welcomeMessage;

    @Column(name = "cover_image_path", length = 255)
    private String coverImagePath;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "primary_music_id")
    private Long primaryMusicId;

    @Column(name = "custom_music_path", length = 255)
    private String customMusicPath;

    @Column(name = "custom_music_title", length = 150)
    private String customMusicTitle;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", insertable = false, updatable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", insertable = false, updatable = false)
    private Template template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_music_id", insertable = false, updatable = false)
    private Music music;

    @OneToMany(mappedBy = "invitation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvitationSession> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "invitation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvitationPerson> persons = new ArrayList<>();

    @OneToMany(mappedBy = "invitation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoveStory> loveStories = new ArrayList<>();

    @OneToMany(mappedBy = "invitation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Gallery> galleries = new ArrayList<>();

    @OneToMany(mappedBy = "invitation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Guest> guests = new ArrayList<>();

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

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public Music getMusic() {
        return music;
    }

    public void setMusic(Music music) {
        this.music = music;
    }

    public List<InvitationSession> getSessions() {
        return sessions;
    }

    public void setSessions(List<InvitationSession> sessions) {
        this.sessions = sessions;
    }

    public List<InvitationPerson> getPersons() {
        return persons;
    }

    public void setPersons(List<InvitationPerson> persons) {
        this.persons = persons;
    }

    public List<LoveStory> getLoveStories() {
        return loveStories;
    }

    public void setLoveStories(List<LoveStory> loveStories) {
        this.loveStories = loveStories;
    }

    public List<Gallery> getGalleries() {
        return galleries;
    }

    public void setGalleries(List<Gallery> galleries) {
        this.galleries = galleries;
    }

    public List<Guest> getGuests() {
        return guests;
    }

    public void setGuests(List<Guest> guests) {
        this.guests = guests;
    }
}
