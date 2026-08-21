package com.undangan.online.dto;

import java.util.List;

public class RoleDto {
    private String code;
    private String name;
    private String description;
    private Boolean isSystem;
    private List<String> permissions;

    public RoleDto() {}

    public RoleDto(String code, String name, String description, Boolean isSystem, List<String> permissions) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.isSystem = isSystem;
        this.permissions = permissions;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
