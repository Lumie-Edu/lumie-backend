package com.lumie.staff.domain.entity;

import com.lumie.staff.domain.vo.AccessLevel;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "staff_permissions")
@IdClass(StaffPermissionId.class)
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class StaffPermission {

    @Id
    @Column(name = "staff_id")
    private Long staffId;

    @Id
    @Column(name = "permission_code", length = 100)
    private String permissionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 10)
    private AccessLevel accessLevel = AccessLevel.NONE;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @PrePersist
    private void initTenantId() {
        if (this.tenantId == null) {
            this.tenantId = TenantContextHolder.getRequiredTenantId();
        }
    }

    public StaffPermission(Long staffId, String permissionCode, AccessLevel accessLevel) {
        this.staffId = staffId;
        this.permissionCode = permissionCode;
        this.accessLevel = accessLevel;
    }

    public static StaffPermission of(Long staffId, String permissionCode, AccessLevel accessLevel) {
        return new StaffPermission(staffId, permissionCode, accessLevel);
    }
}
