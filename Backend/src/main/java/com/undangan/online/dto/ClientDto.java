package com.undangan.online.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class ClientDto {
    private Long id;
    private String code;
    private String name;
    private String phone;
    private String company;
    private String address;
    private LocalDate activatedAt;
    private LocalDate expiresAt;
    private String status;
    private Short durationMonths;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long userCount;
    private Long invitationCount;

    public ClientDto() {}

    public ClientDto(Long id, String code, String name, String phone, String company,
                     String address, LocalDate activatedAt, LocalDate expiresAt,
                     String status, Short durationMonths, String notes,
                     OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.phone = phone;
        this.company = company;
        this.address = address;
        this.activatedAt = activatedAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.durationMonths = durationMonths;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDate getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDate activatedAt) {
        this.activatedAt = activatedAt;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDate expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Short getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Short durationMonths) {
        this.durationMonths = durationMonths;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }

    public Long getInvitationCount() {
        return invitationCount;
    }

    public void setInvitationCount(Long invitationCount) {
        this.invitationCount = invitationCount;
    }
}
