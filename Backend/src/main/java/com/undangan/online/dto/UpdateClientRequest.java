package com.undangan.online.dto;

import jakarta.validation.constraints.Size;

public class UpdateClientRequest {
    @Size(max = 150, message = "Nama maksimal 150 karakter")
    private String name;

    @Size(max = 30, message = "Phone maksimal 30 karakter")
    private String phone;

    @Size(max = 150, message = "Company maksimal 150 karakter")
    private String company;

    private String address;

    private String notes;

    private Short durationMonths;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Short getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Short durationMonths) {
        this.durationMonths = durationMonths;
    }
}
