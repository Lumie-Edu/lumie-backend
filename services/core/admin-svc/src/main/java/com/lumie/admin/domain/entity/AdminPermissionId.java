package com.lumie.admin.domain.entity;

import java.io.Serializable;
import java.util.Objects;

public class AdminPermissionId implements Serializable {

    private Long adminId;
    private String permissionCode;

    public AdminPermissionId() {}

    public AdminPermissionId(Long adminId, String permissionCode) {
        this.adminId = adminId;
        this.permissionCode = permissionCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdminPermissionId that)) return false;
        return Objects.equals(adminId, that.adminId)
                && Objects.equals(permissionCode, that.permissionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adminId, permissionCode);
    }
}
