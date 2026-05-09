package com.lumie.admin.domain.entity;

import com.lumie.admin.domain.vo.AccessLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin_permissions")
@IdClass(AdminPermissionId.class)
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AdminPermission {

    @Id
    @Column(name = "admin_id")
    private Long adminId;

    @Id
    @Column(name = "permission_code", length = 100)
    private String permissionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 10)
    private AccessLevel accessLevel = AccessLevel.NONE;

    public AdminPermission(Long adminId, String permissionCode, AccessLevel accessLevel) {
        this.adminId = adminId;
        this.permissionCode = permissionCode;
        this.accessLevel = accessLevel;
    }

    public static AdminPermission of(Long adminId, String permissionCode, AccessLevel accessLevel) {
        return new AdminPermission(adminId, permissionCode, accessLevel);
    }
}
