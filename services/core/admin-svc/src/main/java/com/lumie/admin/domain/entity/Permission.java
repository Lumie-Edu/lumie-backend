package com.lumie.admin.domain.entity;

import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Permission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "description", length = 255)
    private String description;

    @Builder
    private Permission(String code, String name, String category, String description) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.description = description;
    }

    public static Permission create(String code, String name, String category, String description) {
        return Permission.builder()
                .code(code)
                .name(name)
                .category(category)
                .description(description)
                .build();
    }
}
