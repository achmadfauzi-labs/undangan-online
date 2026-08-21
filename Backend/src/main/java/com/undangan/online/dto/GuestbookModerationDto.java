package com.undangan.online.dto;

import jakarta.validation.constraints.Size;

public class GuestbookModerationDto {
    @Size(max = 500, message = "Reply maksimal 500 karakter")
    private String reply;

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}
