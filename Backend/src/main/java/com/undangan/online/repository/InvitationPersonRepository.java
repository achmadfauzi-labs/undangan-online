package com.undangan.online.repository;

import com.undangan.online.entity.InvitationPerson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvitationPersonRepository extends JpaRepository<InvitationPerson, Long> {
    List<InvitationPerson> findByInvitationId(Long invitationId);
}
