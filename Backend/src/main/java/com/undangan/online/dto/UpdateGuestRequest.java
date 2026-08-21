package com.undangan.online.dto;

import jakarta.validation.constraints.Size;

public class UpdateGuestRequest {
    @Size(max = 150, message = "Nama maksimal 150 karakter")
    private String name;

    @Size(max = 50, message = "Category code maksimal 50 karakter")
    private String categoryCode;

    @jakarta.validation.constraints.Email(message = "Format email tidak valid")
    @Size(max = 150, message = "Email maksimal 150 karakter")
    private String email;

    @Size(max = 30, message = "Phone maksimal 30 karakter")
    private String phone;

    private Short partySize;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Short getPartySize() {
        return partySize;
    }

    public void setPartySize(Short partySize) {
        this.partySize = partySize;
    }
}
