package com.undangan.online.service;

import com.undangan.online.dto.LoginResponse;
import com.undangan.online.dto.RegisterClientRequest;
import com.undangan.online.dto.UserDto;
import com.undangan.online.entity.Users;

public interface AuthService {
    LoginResponse login(String username, String password, String ipAddress, String userAgent);
    UserDto registerClient(RegisterClientRequest request);
    String generateAccessToken(Users user);
    long getAccessTokenExpiresIn();
    long getRefreshTokenExpiresIn();
}
