package com.undangan.online.service;

import com.undangan.online.dto.LoginResponse;
import com.undangan.online.dto.RegisterClientRequest;
import com.undangan.online.dto.UserDto;

public interface AuthService {
    LoginResponse login(String username, String password);
    UserDto registerClient(RegisterClientRequest request);
}
