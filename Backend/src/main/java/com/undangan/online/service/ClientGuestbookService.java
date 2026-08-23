package com.undangan.online.service;

import com.undangan.online.dto.GuestbookEntryDto;
import com.undangan.online.dto.GuestbookModerationDto;

import java.util.List;

public interface ClientGuestbookService {

    List<GuestbookEntryDto> listEntries(Boolean isPublished, String search);

    GuestbookEntryDto getEntry(Long guestId);

    GuestbookEntryDto replyToEntry(Long guestId, GuestbookModerationDto request);

    void deleteEntry(Long guestId);
}
