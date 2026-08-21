package com.undangan.online.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "invitation_person")
public class InvitationPerson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "invitation_id", nullable = false)
    private Long invitationId;

    @Column(name = "role", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private String role;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "parent_names", length = 255)
    private String parentNames;

    @Column(name = "child_order")
    private Short childOrder;

    @Column(name = "photo_path", length = 255)
    private String photoPath;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id", insertable = false, updatable = false)
    private Invitation invitation;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getParentNames() {
        return parentNames;
    }

    public void setParentNames(String parentNames) {
        this.parentNames = parentNames;
    }

    public Short getChildOrder() {
        return childOrder;
    }

    public void setChildOrder(Short childOrder) {
        this.childOrder = childOrder;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
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

    public Invitation getInvitation() {
        return invitation;
    }

    public void setInvitation(Invitation invitation) {
        this.invitation = invitation;
    }
}
