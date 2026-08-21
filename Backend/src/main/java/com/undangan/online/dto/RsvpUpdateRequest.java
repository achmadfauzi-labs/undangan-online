package com.undangan.online.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RsvpUpdateRequest {
    @NotNull(message = "Attendance status wajib diisi")
    private String attendanceStatus;

    private Short partySize;

    @Size(max = 500, message = "Message maksimal 500 karakter")
    private String message;

    public String getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public Short getPartySize() {
        return partySize;
    }

    public void setPartySize(Short partySize) {
        this.partySize = partySize;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
