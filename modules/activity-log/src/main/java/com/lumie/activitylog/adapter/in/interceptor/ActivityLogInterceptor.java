package com.lumie.activitylog.adapter.in.interceptor;

import com.lumie.activitylog.application.service.ActivityLogContext;
import com.lumie.common.auth.ActionType;
import com.lumie.common.auth.EntityType;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.activitylog.api.ActivityLogService;
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

    private static final Pattern ENTITY_ID_PATTERN = Pattern.compile("/v1/[^/]+/(\\d+)");
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final Map<String, EntityType> PATH_TO_ENTITY = new LinkedHashMap<>();

    static {
        PATH_TO_ENTITY.put("/v1/classes/*/enrollments/**", EntityType.ENROLLMENT);
        PATH_TO_ENTITY.put("/v1/classes/**", EntityType.CLASS);
        PATH_TO_ENTITY.put("/v1/students/**", EntityType.STUDENT);
        PATH_TO_ENTITY.put("/v1/admins/**", EntityType.ADMIN);
        PATH_TO_ENTITY.put("/v1/academies/**", EntityType.ACADEMY);
        PATH_TO_ENTITY.put("/v1/attendance/**", EntityType.ATTENDANCE);
        PATH_TO_ENTITY.put("/v1/assignments/**", EntityType.ASSIGNMENT);
        PATH_TO_ENTITY.put("/v1/exams/**", EntityType.EXAM);
        PATH_TO_ENTITY.put("/v1/exam-templates/**", EntityType.EXAM_TEMPLATE);
        PATH_TO_ENTITY.put("/v1/lectures/**", EntityType.LECTURE);
        PATH_TO_ENTITY.put("/v1/textbooks/**", EntityType.TEXTBOOK);
        PATH_TO_ENTITY.put("/v1/schedules/**", EntityType.SCHEDULE);
        PATH_TO_ENTITY.put("/v1/announcements/**", EntityType.ANNOUNCEMENT);
        PATH_TO_ENTITY.put("/v1/reviews/**", EntityType.REVIEW);
        PATH_TO_ENTITY.put("/v1/reservations/**", EntityType.RESERVATION);
        PATH_TO_ENTITY.put("/v1/sms/**", EntityType.SMS);
        PATH_TO_ENTITY.put("/v1/qna/**", EntityType.QNA);
        PATH_TO_ENTITY.put("/v1/conversations/**", EntityType.CONVERSATION);
        PATH_TO_ENTITY.put("/v1/chat/**", EntityType.CHAT);
        PATH_TO_ENTITY.put("/v1/files/**", EntityType.FILE);
        PATH_TO_ENTITY.put("/v1/scheduled-tasks/**", EntityType.SCHEDULED_TASK);
        PATH_TO_ENTITY.put("/v1/statistics/**", EntityType.STATISTICS);
        PATH_TO_ENTITY.put("/v1/reports/**", EntityType.REPORT);
        PATH_TO_ENTITY.put("/v1/results/**", EntityType.RESULT);
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

            EntityType entityType = resolveEntityType(path);
            if (entityType == null) {
                return;
            }

            ActionType action = resolveAction(method, path);
            if (action == null) {
                return;
            }
            Long entityId = extractEntityId(path);

            String tenantSlug = TenantContextHolder.getTenant();
            com.lumie.common.auth.Role role = UserContextHolder.getUserRole();
            if (tenantSlug == null || role == null) {
                return;
            }

            // Snapshot pattern (CIRCULAR_MODULE_DEPS_AUDIT Resolution A, pair 1):
            // log directly from request context — no cross-module lookup.
            // Actor display name will be filled from JWT name claim once that is added
            // to the auth pipeline; until then we log actorId only and the UI resolves.
            String actorName = UserContextHolder.getUserName();
            activityLogService.log(
                    userId, actorName, role.name(),
                    action.name(), entityType.name(), entityId, null, null
            );
        } catch (Exception e) {
            log.warn("Failed to auto-log activity: {}", e.getMessage());
        } finally {
            ActivityLogContext.clear();
        }
    }

    private EntityType resolveEntityType(String path) {
        for (Map.Entry<String, EntityType> entry : PATH_TO_ENTITY.entrySet()) {
            if (PATH_MATCHER.match(entry.getKey(), path)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private ActionType resolveAction(String method, String path) {
        return switch (method) {
            case "GET" -> ActionType.VIEW;
            case "DELETE" -> ActionType.DELETE;
            case "PUT", "PATCH" -> ActionType.UPDATE;
            case "POST" -> resolvePostAction(path);
            default -> null;
        };
    }

    private ActionType resolvePostAction(String path) {
        if (path.endsWith("/deactivate")) return ActionType.DEACTIVATE;
        if (path.endsWith("/reactivate")) return ActionType.REACTIVATE;
        if (path.endsWith("/terminate")) return ActionType.TERMINATE;
        if (path.endsWith("/check-in")) return ActionType.CHECK_IN;
        if (path.contains("/send")) return ActionType.SEND;
        if (path.endsWith("/close")) return ActionType.CLOSE;
        return ActionType.CREATE;
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
