package com.undangan.online.service;

import com.undangan.online.dto.CreateUserRequest;
import com.undangan.online.dto.PageResponse;
import com.undangan.online.dto.ResetPasswordRequest;
import com.undangan.online.dto.UpdateUserRequest;
import com.undangan.online.dto.UpdateUserStatusRequest;
import com.undangan.online.dto.UserListDto;
import org.springframework.data.domain.Pageable;

public interface UserService {

    PageResponse<UserListDto> list(Long clientId, String roleCode, String status, String search, Pageable pageable);

    UserListDto getById(Long id);

    UserListDto create(CreateUserRequest request);

    UserListDto update(Long id, UpdateUserRequest request);

    void resetPassword(Long id, ResetPasswordRequest request);

    UserListDto updateStatus(Long id, UpdateUserStatusRequest request);

    void delete(Long id);

    PageResponse<UserListDto> listByClient(Long clientId, Pageable pageable);
}
