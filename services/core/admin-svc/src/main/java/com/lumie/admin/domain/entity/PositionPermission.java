package com.lumie.admin.domain.entity;

import com.lumie.admin.domain.vo.AccessLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "position_permissions")
@IdClass(PositionPermissionId.class)
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PositionPermission {

    @Id
    @Column(name = "position_id")
    private Long positionId;

    @Id
    @Column(name = "permission_code", length = 100)
    private String permissionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 10)
    private AccessLevel accessLevel = AccessLevel.NONE;

    public PositionPermission(Long positionId, String permissionCode, AccessLevel accessLevel) {
        this.positionId = positionId;
        this.permissionCode = permissionCode;
        this.accessLevel = accessLevel;
    }

    public static PositionPermission of(Long positionId, String permissionCode, AccessLevel accessLevel) {
        return new PositionPermission(positionId, permissionCode, accessLevel);
    }
}
