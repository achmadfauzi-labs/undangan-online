package com.undangan.online.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdateInvitationSessionRequest {
    @Size(max = 100, message = "Nama maksimal 100 karakter")
    private String name;

    private LocalDate sessionDate;

    @Size(max = 50, message = "Session time maksimal 50 karakter")
    private String sessionTime;

    private String location;

    @Size(max = 500, message = "Maps URL maksimal 500 karakter")
    private String mapsUrl;

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
