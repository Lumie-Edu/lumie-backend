package com.lumie.admin.domain.entity;

import java.io.Serializable;
import java.util.Objects;

public class PositionPermissionId implements Serializable {

    private Long positionId;
    private String permissionCode;

    public PositionPermissionId() {}

    public PositionPermissionId(Long positionId, String permissionCode) {
        this.positionId = positionId;
        this.permissionCode = permissionCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PositionPermissionId that)) return false;
        return Objects.equals(positionId, that.positionId)
                && Objects.equals(permissionCode, that.permissionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(positionId, permissionCode);
    }
}
