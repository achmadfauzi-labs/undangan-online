package com.undangan.online.service.impl;

import com.undangan.online.entity.RefreshToken;
import com.undangan.online.entity.Users;
import com.undangan.online.exception.AuthException;
import com.undangan.online.repository.RefreshTokenRepository;
import com.undangan.online.repository.UsersRepository;
import com.undangan.online.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UsersRepository usersRepository;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, UsersRepository usersRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.usersRepository = usersRepository;
    }

    @Override
    public String generateRefreshToken(Long userId, String userAgent, String ipAddress) {
        String token = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUserId(userId);
        refreshToken.setIssuedAt(now);
        refreshToken.setExpiresAt(now.plusNanos(refreshExpirationMs * 1_000_000));
        refreshToken.setUserAgent(userAgent);
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setCreatedAt(now);

        refreshTokenRepository.save(refreshToken);
        log.info("Refresh token generated for userId={}", userId);
        return token;
    }

    @Override
    public Users validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElse(null);

        if (refreshToken == null) {
            log.warn("Refresh token not found: {}", token);
            throw new AuthException("INVALID_TOKEN", "Refresh token tidak valid atau expired", 401);
        }

        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("Refresh token expired for userId={}", refreshToken.getUserId());
            refreshTokenRepository.delete(refreshToken);
            throw new AuthException("INVALID_TOKEN", "Refresh token tidak valid atau expired", 401);
        }

        Users user = usersRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new AuthException("INVALID_TOKEN", "User tidak ditemukan", 401));
        return user;
    }

    @Override
    @Transactional
    public String rotateRefreshToken(String oldToken, String userAgent, String ipAddress) {
        RefreshToken oldRefreshToken = refreshTokenRepository.findByToken(oldToken)
                .orElseThrow(() -> new AuthException("INVALID_TOKEN", "Refresh token tidak valid atau expired", 401));

        if (oldRefreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            refreshTokenRepository.delete(oldRefreshToken);
            throw new AuthException("INVALID_TOKEN", "Refresh token tidak valid atau expired", 401);
        }

        Long userId = oldRefreshToken.getUserId();
        refreshTokenRepository.delete(oldRefreshToken);

        log.info("Refresh token rotated for userId={}", userId);
        return generateRefreshToken(userId, userAgent, ipAddress);
    }

    @Override
    @Transactional
    public void deleteRefreshToken(String token) {
        refreshTokenRepository.deleteByToken(token);
        log.info("Refresh token deleted: {}", token);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("All refresh tokens deleted for userId={}", userId);
    }
}
