package com.lumie.attendance.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.classroom.api.ClassService;
import com.lumie.classroom.api.ClassService.ClassData;
import com.lumie.classroom.api.ClassService.ClassListData;
import com.lumie.tenant.api.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceSessionScheduler {

    private final AttendanceSessionCommandService commandService;
    private final ClassService classService;
    private final TenantService tenantService;

    private static final Map<DayOfWeek, String> DAY_OF_WEEK_MAP = Map.of(
        DayOfWeek.MONDAY, "MON",
        DayOfWeek.TUESDAY, "TUE",
        DayOfWeek.WEDNESDAY, "WED",
        DayOfWeek.THURSDAY, "THU",
        DayOfWeek.FRIDAY, "FRI",
        DayOfWeek.SATURDAY, "SAT",
        DayOfWeek.SUNDAY, "SUN"
    );

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "createDailyAttendanceSessions",
            lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void createDailyAttendanceSessions() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        String todayDay = DAY_OF_WEEK_MAP.get(today.getDayOfWeek());

        log.info("Starting daily attendance session creation for {} ({})", today, todayDay);

        List<String> tenantSlugs = tenantService.listActiveTenantSlugs();
        int totalCreated = 0;

        for (String slug : tenantSlugs) {
            try {
                TenantContextHolder.setTenant(slug);
                int created = createSessionsForTenant(slug, today, todayDay);
                totalCreated += created;
            } catch (Exception e) {
                log.error("Failed to create attendance sessions for tenant {}: {}", slug, e.getMessage());
            } finally {
                TenantContextHolder.clear();
            }
        }

        log.info("Daily attendance session creation completed. Total created: {}", totalCreated);
    }

    private int createSessionsForTenant(String tenantSlug, LocalDate today, String todayDay) {
        int created = 0;
        int page = 0;
        int size = 100;

        while (true) {
            ClassListData classListData = classService.getAllClasses(tenantSlug, page, size);
            List<ClassData> classes = classListData.classes();

            if (classes.isEmpty()) break;

            for (ClassData cls : classes) {
                if (cls.scheduleDays() != null && cls.scheduleDays().contains(todayDay)) {
                    try {
                        commandService.createSessionForClass(
                            cls.id(), cls.name(), today, 10
                        );
                        created++;
                    } catch (Exception e) {
                        log.error("Failed to create session for class {} in tenant {}: {}",
                            cls.id(), tenantSlug, e.getMessage());
                    }
                }
            }

            if (classes.size() < size) break;
            page++;
        }

        return created;
    }

}
