package com.undangan.online.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateInvitationSessionRequest {
    @NotBlank(message = "Nama sesi wajib diisi")
    @Size(max = 100, message = "Nama maksimal 100 karakter")
    private String name;

    @NotNull(message = "Tanggal sesi wajib diisi")
    private LocalDate sessionDate;

    @NotBlank(message = "Session time wajib diisi")
    @Size(max = 50, message = "Session time maksimal 50 karakter")
    private String sessionTime;

    @NotBlank(message = "Lokasi wajib diisi")
    @Size(max = 255, message = "Lokasi maksimal 255 karakter")
    private String location;

    @Size(max = 500, message = "Maps URL maksimal 500 karakter")
    private String mapsUrl;

    @NotNull(message = "Sort order wajib diisi")
    private Short sortOrder;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public String getSessionTime() {
        return sessionTime;
    }

    public void setSessionTime(String sessionTime) {
        this.sessionTime = sessionTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getMapsUrl() {
        return mapsUrl;
    }

    public void setMapsUrl(String mapsUrl) {
        this.mapsUrl = mapsUrl;
    }

    public Short getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Short sortOrder) {
        this.sortOrder = sortOrder;
    }
}
