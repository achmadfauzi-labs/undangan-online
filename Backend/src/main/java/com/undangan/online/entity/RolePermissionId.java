package com.undangan.online.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RolePermissionId implements Serializable {
    @Column(name = "role_code", length = 30)
    private String roleCode;

    @Column(name = "menu_key", length = 50)
    private String menuKey;

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getMenuKey() {
        return menuKey;
    }

    public void setMenuKey(String menuKey) {
        this.menuKey = menuKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RolePermissionId that = (RolePermissionId) o;
        return Objects.equals(roleCode, that.roleCode) && Objects.equals(menuKey, that.menuKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleCode, menuKey);
    }
}
