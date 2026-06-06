package com.lumie.classroom.domain.vo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public record SchedulePattern(
        List<String> days,
        String startTime,
        String endTime,
        List<ScheduleSession> sessions
) {
    public SchedulePattern {
        List<ScheduleSession> normalizedSessions = normalizeSessions(days, startTime, endTime, sessions);
        sessions = List.copyOf(normalizedSessions);
        LinkedHashSet<String> normalizedDays = new LinkedHashSet<>();
        for (ScheduleSession session : normalizedSessions) {
            normalizedDays.add(session.day());
        }
        days = List.copyOf(normalizedDays);
        startTime = sharedStartTime(normalizedSessions);
        endTime = sharedEndTime(normalizedSessions);
    }

    public SchedulePattern(List<String> days, String startTime, String endTime) {
        this(days, startTime, endTime, null);
    }

    private static List<ScheduleSession> normalizeSessions(
            List<String> days,
            String startTime,
            String endTime,
            List<ScheduleSession> sessions
    ) {
        if (sessions != null && !sessions.isEmpty()) {
            return sessions;
        }
        if (days == null || days.isEmpty() || startTime == null || endTime == null) {
            return List.of();
        }
        List<ScheduleSession> legacySessions = new ArrayList<>();
        for (String day : days) {
            legacySessions.add(new ScheduleSession(day, startTime, endTime));
        }
        return legacySessions;
    }

    private static String sharedStartTime(List<ScheduleSession> sessions) {
        if (sessions.isEmpty()) {
            return null;
        }
        String firstStartTime = sessions.getFirst().startTime();
        boolean allShareStartTime = sessions.stream()
                .allMatch(session -> firstStartTime.equals(session.startTime()));
        return allShareStartTime ? firstStartTime : null;
    }

    private static String sharedEndTime(List<ScheduleSession> sessions) {
        if (sessions.isEmpty()) {
            return null;
        }
        String firstEndTime = sessions.getFirst().endTime();
        boolean allShareEndTime = sessions.stream()
                .allMatch(session -> firstEndTime.equals(session.endTime()));
        return allShareEndTime ? firstEndTime : null;
    }

    public record ScheduleSession(
            String day,
            String startTime,
            String endTime
    ) {
    }
}
