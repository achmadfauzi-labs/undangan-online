package com.undangan.online.repository;

import com.undangan.online.entity.InvitationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvitationSessionRepository extends JpaRepository<InvitationSession, Long> {
    List<InvitationSession> findByInvitationId(Long invitationId);
}
