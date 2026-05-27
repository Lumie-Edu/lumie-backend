package com.lumie.tuition.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "guardians", indexes = {
        @Index(name = "idx_guardians_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_guardians_phone", columnList = "phone")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guardian extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "relationship", length = 50)
    private String relationship;

    @Builder
    private Guardian(String name, String phone, String email, String relationship) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.relationship = relationship;
    }

    public static Guardian create(String name, String phone, String email, String relationship) {
        return Guardian.builder()
                .name(name)
                .phone(phone)
                .email(email)
                .relationship(relationship)
                .build();
    }

    public void updateInfo(String name, String phone, String email, String relationship) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (phone != null && !phone.isBlank()) {
            this.phone = phone;
        }
        if (email != null) {
            this.email = email;
        }
        if (relationship != null) {
            this.relationship = relationship;
        }
    }
}
