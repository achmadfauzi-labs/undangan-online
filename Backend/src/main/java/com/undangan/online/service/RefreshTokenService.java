package com.undangan.online.service;

import com.undangan.online.entity.Users;

public interface RefreshTokenService {

    String generateRefreshToken(Long userId, String userAgent, String ipAddress);

    Users validateRefreshToken(String token);

    String rotateRefreshToken(String oldToken, String userAgent, String ipAddress);

    void deleteRefreshToken(String token);

    void deleteByUserId(Long userId);
}
