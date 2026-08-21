package com.undangan.online.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class UpdateRolePermissionsRequest {
    @NotNull(message = "Permissions wajib diisi")
    @Size(min = 0, message = "Permissions tidak boleh null")
    private List<String> permissions;

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
