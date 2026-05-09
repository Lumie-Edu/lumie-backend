package com.lumie.activitylog.adapter.in.interceptor;

import com.lumie.activitylog.application.service.ActivityLogContext;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.internal.activitylog.ActivityLogService;
import com.lumie.internal.admin.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityLogInterceptor implements HandlerInterceptor {

    private final ActivityLogService activityLogService;
    private final AdminService adminService;

    private static final Pattern ENTITY_ID_PATTERN = Pattern.compile("/v1/[^/]+/(\\d+)");
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final Map<String, String> PATH_TO_ENTITY = new LinkedHashMap<>();

    static {
        PATH_TO_ENTITY.put("/v1/classes/*/enrollments/**", "ENROLLMENT");
        PATH_TO_ENTITY.put("/v1/classes/**", "CLASS");
        PATH_TO_ENTITY.put("/v1/students/**", "STUDENT");
        PATH_TO_ENTITY.put("/v1/admins/**", "ADMIN");
        PATH_TO_ENTITY.put("/v1/academies/**", "ACADEMY");
        PATH_TO_ENTITY.put("/v1/attendance/**", "ATTENDANCE");
        PATH_TO_ENTITY.put("/v1/assignments/**", "ASSIGNMENT");
        PATH_TO_ENTITY.put("/v1/exams/**", "EXAM");
        PATH_TO_ENTITY.put("/v1/exam-templates/**", "EXAM_TEMPLATE");
        PATH_TO_ENTITY.put("/v1/lectures/**", "LECTURE");
        PATH_TO_ENTITY.put("/v1/textbooks/**", "TEXTBOOK");
        PATH_TO_ENTITY.put("/v1/schedules/**", "SCHEDULE");
        PATH_TO_ENTITY.put("/v1/announcements/**", "ANNOUNCEMENT");
        PATH_TO_ENTITY.put("/v1/reviews/**", "REVIEW");
        PATH_TO_ENTITY.put("/v1/reservations/**", "RESERVATION");
        PATH_TO_ENTITY.put("/v1/sms/**", "SMS");
        PATH_TO_ENTITY.put("/v1/qna/**", "QNA");
        PATH_TO_ENTITY.put("/v1/conversations/**", "CONVERSATION");
        PATH_TO_ENTITY.put("/v1/chat/**", "CHAT");
        PATH_TO_ENTITY.put("/v1/files/**", "FILE");
        PATH_TO_ENTITY.put("/v1/scheduled-tasks/**", "SCHEDULED_TASK");
        PATH_TO_ENTITY.put("/v1/statistics/**", "STATISTICS");
        PATH_TO_ENTITY.put("/v1/reports/**", "REPORT");
        PATH_TO_ENTITY.put("/v1/results/**", "RESULT");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            if (ActivityLogContext.isLogged()) {
                return;
            }

            Long userId = UserContextHolder.getUserId();
            if (userId == null) {
                return;
            }

            int status = response.getStatus();
            if (status >= 400) {
                return;
            }

            String path = request.getRequestURI();
            String method = request.getMethod();

            String entityType = resolveEntityType(path);
            if (entityType == null) {
                return;
            }

            String action = resolveAction(method, path);
            Long entityId = extractEntityId(path);

            String tenantSlug = TenantContextHolder.getTenant();
            String role = UserContextHolder.getUserRole();
            if (tenantSlug == null || role == null) {
                return;
            }

            adminService.getAdminByUserId(tenantSlug, userId).ifPresent(actor ->
                    activityLogService.log(
                            actor.id(), actor.name(), role,
                            action, entityType, entityId, null, null
                    )
            );
        } catch (Exception e) {
            log.warn("Failed to auto-log activity: {}", e.getMessage());
        } finally {
            ActivityLogContext.clear();
        }
    }

    private String resolveEntityType(String path) {
        for (Map.Entry<String, String> entry : PATH_TO_ENTITY.entrySet()) {
            if (PATH_MATCHER.match(entry.getKey(), path)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String resolveAction(String method, String path) {
        return switch (method) {
            case "GET" -> "VIEW";
            case "DELETE" -> "DELETE";
            case "PUT", "PATCH" -> "UPDATE";
            case "POST" -> resolvePostAction(path);
            default -> method;
        };
    }

    private String resolvePostAction(String path) {
        if (path.endsWith("/deactivate")) return "DEACTIVATE";
        if (path.endsWith("/reactivate")) return "REACTIVATE";
        if (path.endsWith("/terminate")) return "TERMINATE";
        if (path.endsWith("/check-in")) return "CHECK_IN";
        if (path.contains("/send")) return "SEND";
        if (path.endsWith("/close")) return "CLOSE";
        return "CREATE";
    }

    private Long extractEntityId(String path) {
        Matcher matcher = ENTITY_ID_PATTERN.matcher(path);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
