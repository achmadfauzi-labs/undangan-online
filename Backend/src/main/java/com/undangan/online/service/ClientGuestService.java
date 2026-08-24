package com.undangan.online.service;

import com.undangan.online.dto.CreateGuestRequest;
import com.undangan.online.dto.GuestDto;
import com.undangan.online.dto.UpdateGuestRequest;

import java.util.List;
import java.util.Map;

public interface ClientGuestService {

    List<GuestDto> listGuests(String status, String search);

    GuestDto getGuest(Long guestId);

    GuestDto createGuest(CreateGuestRequest request);

    GuestDto updateGuest(Long guestId, UpdateGuestRequest request);

    void deleteGuest(Long guestId);

    List<GuestDto> bulkCreateGuests(List<CreateGuestRequest> requests);

    Map<String, Object> generateGuestLink(Long guestId);

    List<GuestDto> exportGuests();
}
