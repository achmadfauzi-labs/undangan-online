package com.undangan.online.dto;

public class UserDto {
    private Long id;
    private String username;
    private String name;
    private String roleCode;
    private Long clientId;

    public UserDto(Long id, String username, String name, String roleCode, Long clientId) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.roleCode = roleCode;
        this.clientId = clientId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }
}
