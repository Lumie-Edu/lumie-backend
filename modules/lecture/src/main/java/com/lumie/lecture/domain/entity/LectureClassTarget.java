package com.lumie.lecture.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "lecture_class_targets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "lecture_class_targets_lecture_class_key",
                        columnNames = {"lecture_id", "class_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureClassTarget extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Builder
    private LectureClassTarget(Lecture lecture, Long classId) {
        this.lecture = lecture;
        this.classId = classId;
    }

    public static LectureClassTarget create(Lecture lecture, Long classId) {
        return LectureClassTarget.builder()
                .lecture(lecture)
                .classId(classId)
                .build();
    }
}
