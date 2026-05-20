package com.lumie.staff.domain.entity;

import java.io.Serializable;
import java.util.Objects;

public class StaffPermissionId implements Serializable {

    private Long staffId;
    private String permissionCode;

    public StaffPermissionId() {}

    public StaffPermissionId(Long staffId, String permissionCode) {
        this.staffId = staffId;
        this.permissionCode = permissionCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StaffPermissionId that)) return false;
        return Objects.equals(staffId, that.staffId)
                && Objects.equals(permissionCode, that.permissionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(staffId, permissionCode);
    }
}
