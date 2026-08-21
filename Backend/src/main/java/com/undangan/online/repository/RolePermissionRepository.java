package com.undangan.online.repository;

import com.undangan.online.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, com.undangan.online.entity.RolePermissionId> {
    List<RolePermission> findByRole_Code(String roleCode);
    void deleteByRole_Code(String roleCode);
}
