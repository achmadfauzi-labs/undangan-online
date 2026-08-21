package com.undangan.online.repository;

import com.undangan.online.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    Optional<Guest> findByInvitationToken(String invitationToken);

    List<Guest> findByInvitationId(Long invitationId);

    List<Guest> findByInvitationIdAndIsPublishedTrue(Long invitationId, Sort sort);
}
