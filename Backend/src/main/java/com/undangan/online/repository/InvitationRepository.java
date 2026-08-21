package com.undangan.online.repository;

import com.undangan.online.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findBySlug(String slug);

    Optional<Invitation> findByClientId(Long clientId);

    List<Invitation> findByStatus(String status);
}
