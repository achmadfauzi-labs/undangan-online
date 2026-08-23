package com.undangan.online.service;

import com.undangan.online.dto.CreateInvitationPersonRequest;
import com.undangan.online.dto.CreateInvitationSessionRequest;
import com.undangan.online.dto.CreateLoveStoryRequest;
import com.undangan.online.dto.InvitationDto;
import com.undangan.online.dto.InvitationPersonDto;
import com.undangan.online.dto.InvitationSessionDto;
import com.undangan.online.dto.LoveStoryDto;
import com.undangan.online.dto.UpdateInvitationPersonRequest;
import com.undangan.online.dto.UpdateInvitationRequest;
import com.undangan.online.dto.UpdateInvitationSessionRequest;
import com.undangan.online.dto.UpdateLoveStoryRequest;

import java.util.List;
import java.util.Map;

public interface ClientInvitationService {

    InvitationDto getMyInvitation();

    InvitationDto updateInvitation(UpdateInvitationRequest request);

    Map<String, String> generateSlug(String baseSlug);

    void publishInvitation();

    List<InvitationPersonDto> listPersons();

    InvitationPersonDto getPerson(Long personId);

    InvitationPersonDto createPerson(CreateInvitationPersonRequest request);

    InvitationPersonDto updatePerson(Long personId, UpdateInvitationPersonRequest request);

    void deletePerson(Long personId);

    List<InvitationSessionDto> listSessions();

    InvitationSessionDto getSession(Long sessionId);

    InvitationSessionDto createSession(CreateInvitationSessionRequest request);

    InvitationSessionDto updateSession(Long sessionId, UpdateInvitationSessionRequest request);

    void deleteSession(Long sessionId);

    void reorderSessions(List<Map<String, Integer>> order);

    List<LoveStoryDto> listLoveStories();

    LoveStoryDto getLoveStory(Long loveStoryId);

    LoveStoryDto createLoveStory(CreateLoveStoryRequest request);

    LoveStoryDto updateLoveStory(Long loveStoryId, UpdateLoveStoryRequest request);

    void deleteLoveStory(Long loveStoryId);

    Map<String, Object> getPublicInvitation(String slug);

    List<Map<String, Object>> getPublicGuestbook(String slug);
}
