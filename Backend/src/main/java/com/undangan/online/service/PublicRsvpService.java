package com.undangan.online.service;

import com.undangan.online.dto.RsvpSubmitRequest;

import java.util.List;
import java.util.Map;

public interface PublicRsvpService {

    Map<String, Object> submitRsvp(RsvpSubmitRequest request);

    Map<String, Object> getRsvpStatus(String token);

    Map<String, Object> submitGuestbook(String slug, String name, String message);

    List<Map<String, Object>> getGuestbook(String slug);
}
