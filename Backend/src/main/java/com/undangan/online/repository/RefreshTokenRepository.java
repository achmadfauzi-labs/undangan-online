package com.undangan.online.repository;

import com.undangan.online.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByUserId(Long userId);

    List<RefreshToken> findAllByUserIdAndExpiresAtAfter(Long userId, OffsetDateTime expiresAt);
}
