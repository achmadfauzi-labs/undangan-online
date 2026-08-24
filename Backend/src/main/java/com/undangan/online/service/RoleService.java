package com.undangan.online.service;

import com.undangan.online.dto.CreateRoleRequest;
import com.undangan.online.dto.RoleDto;
import com.undangan.online.dto.UpdateRolePermissionsRequest;

import java.util.List;

public interface RoleService {

    List<RoleDto> listAll();

    RoleDto getByCode(String code);

    RoleDto create(CreateRoleRequest request);

    RoleDto update(String code, CreateRoleRequest request);

    RoleDto updatePermissions(String code, UpdateRolePermissionsRequest request);

    void delete(String code);

    List<String> listPermissionOptions();

    void seedDefaultRoles();
}
