package com.lumie.attendance.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "attendance_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceSession extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "attendance_code", nullable = false, length = 6)
    private String attendanceCode;

    @Column(name = "code_expires_at")
    private Instant codeExpiresAt;

    @Column(name = "late_threshold_minutes", nullable = false)
    private Integer lateThresholdMinutes;

    // @BatchSize avoids the N+1 lazy-load that would otherwise hit when a Page<AttendanceSession>
    // result iterates over records. Fetch joining via @EntityGraph would force in-memory pagination
    // (HHH000104), so batch fetching is the canonical pattern for Page + OneToMany.
    @BatchSize(size = 25)
    @OneToMany(mappedBy = "session", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<AttendanceRecord> records = new ArrayList<>();

    @Builder
    private AttendanceSession(String name, LocalDate sessionDate, Long classId,
                              String className, String attendanceCode, Instant codeExpiresAt,
                              Integer lateThresholdMinutes) {
        this.name = name;
        this.sessionDate = sessionDate;
        this.classId = classId;
        this.className = className;
        this.attendanceCode = attendanceCode;
        this.codeExpiresAt = codeExpiresAt;
        this.lateThresholdMinutes = lateThresholdMinutes != null ? lateThresholdMinutes : 10;
    }

    public static AttendanceSession create(String name, LocalDate sessionDate,
                                           Long classId, String className,
                                           Integer lateThresholdMinutes) {
        return AttendanceSession.builder()
                .name(name)
                .sessionDate(sessionDate)
                .classId(classId)
                .className(className)
                .attendanceCode(generateCode())
                .lateThresholdMinutes(lateThresholdMinutes)
                .build();
    }

    public void regenerateCode() {
        this.attendanceCode = generateCode();
    }

    public boolean isExpired() {
        return codeExpiresAt != null && Instant.now().isAfter(codeExpiresAt);
    }

    private static String generateCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }
}
