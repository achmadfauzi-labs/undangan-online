package com.undangan.online.repository;

import com.undangan.online.entity.Gallery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GalleryRepository extends JpaRepository<Gallery, Long> {
    List<Gallery> findByInvitationId(Long invitationId);
    List<Gallery> findByInvitationIdOrderBySortOrderAsc(Long invitationId);
}
