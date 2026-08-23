package com.undangan.online.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateInvitationPersonRequest {
    @NotBlank(message = "Role wajib diisi")
    @Pattern(regexp = "^(groom|bride)$", message = "Role harus 'groom' atau 'bride'")
    private String role;

    @NotBlank(message = "Nama wajib diisi")
    @Size(max = 150, message = "Nama maksimal 150 karakter")
    private String name;

    @Size(max = 50, message = "Nickname maksimal 50 karakter")
    private String nickname;

    @Size(max = 255, message = "Parent names maksimal 255 karakter")
    private String parentNames;

    private Short childOrder;

    @Size(max = 255, message = "Photo path maksimal 255 karakter")
    private String photoPath;

    @NotNull(message = "Sort order wajib diisi")
    private Short sortOrder;

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
}
