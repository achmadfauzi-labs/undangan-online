package com.undangan.online.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateSystemParameterValueRequest {
    @NotBlank(message = "Group code wajib diisi")
    @Size(max = 50, message = "Group code maksimal 50 karakter")
    private String groupCode;

    @NotBlank(message = "Code wajib diisi")
    @Size(max = 50, message = "Code maksimal 50 karakter")
    private String code;

    @NotBlank(message = "Value wajib diisi")
    @Size(max = 150, message = "Value maksimal 150 karakter")
    private String value;

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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
