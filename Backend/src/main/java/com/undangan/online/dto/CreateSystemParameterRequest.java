package com.undangan.online.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateSystemParameterRequest {
    @NotBlank(message = "Group code wajib diisi")
    @Size(max = 50, message = "Group code maksimal 50 karakter")
    private String groupCode;

    @NotBlank(message = "Code wajib diisi")
    @Size(max = 50, message = "Code maksimal 50 karakter")
    private String code;

    @NotBlank(message = "Name wajib diisi")
    @Size(max = 150, message = "Name maksimal 150 karakter")
    private String name;

    @NotNull(message = "Sort order wajib diisi")
    private Short sortOrder;

    private String status;

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
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

    public Short getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Short sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
