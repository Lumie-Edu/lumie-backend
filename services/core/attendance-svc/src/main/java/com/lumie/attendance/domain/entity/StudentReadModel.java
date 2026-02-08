package com.lumie.attendance.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "students")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentReadModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "academy_id")
    private Long academyId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "user_id")
    private Long userId;
}
