package com.undangan.online.dto;

import jakarta.validation.constraints.Size;

public class UpdateInvitationRequest {
    @Size(max = 500, message = "Welcome message maksimal 500 karakter")
    private String welcomeMessage;

    @Size(max = 50, message = "Event type code maksimal 50 karakter")
    private String eventTypeCode;

    @Size(max = 20, message = "Status maksimal 20 karakter")
    private String status;

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public String getEventTypeCode() {
        return eventTypeCode;
    }

    public void setEventTypeCode(String eventTypeCode) {
        this.eventTypeCode = eventTypeCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
