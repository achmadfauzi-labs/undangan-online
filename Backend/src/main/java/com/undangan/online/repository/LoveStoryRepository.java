package com.undangan.online.repository;

import com.undangan.online.entity.LoveStory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoveStoryRepository extends JpaRepository<LoveStory, Long> {
    List<LoveStory> findByInvitationId(Long invitationId);
}
