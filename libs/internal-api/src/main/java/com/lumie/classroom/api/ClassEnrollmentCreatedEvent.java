package com.lumie.classroom.api;

import java.util.List;

/** Published after students join a class so active class-targeted assignments can include them. */
public record ClassEnrollmentCreatedEvent(Long tenantId, String tenantSlug, Long classId,
                                          List<Long> studentIds) {
}
