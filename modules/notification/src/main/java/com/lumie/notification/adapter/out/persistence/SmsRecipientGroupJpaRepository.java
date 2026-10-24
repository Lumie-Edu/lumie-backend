package com.lumie.notification.adapter.out.persistence;

import com.lumie.notification.domain.entity.SmsRecipientGroup;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SmsRecipientGroupJpaRepository extends JpaRepository<SmsRecipientGroup, Long> {

    Optional<SmsRecipientGroup> findByIdAndOwnerStaffId(Long id, Long ownerStaffId);

    @Query(value = """
            select
                g.id as id,
                g.name as name,
                g.icon as icon,
                jsonb_array_length(g.recipients) as recipientCount,
                (
                    select count(*)::int
                    from jsonb_array_elements(g.recipients) recipient
                    where recipient ->> 'recipientRole' = 'STUDENT'
                ) as studentRecipientCount,
                (
                    select count(*)::int
                    from jsonb_array_elements(g.recipients) recipient
                    where recipient ->> 'recipientRole' = 'PARENT'
                ) as parentRecipientCount,
                g.created_at as createdAt,
                g.updated_at as updatedAt
            from sms_recipient_groups g
            where g.owner_staff_id = :ownerStaffId
            order by g.created_at desc
            """, nativeQuery = true)
    List<SmsRecipientGroupSummaryProjection> findAllSummariesByOwnerStaffIdOrderByCreatedAtDesc(
            @Param("ownerStaffId") Long ownerStaffId
    );

    interface SmsRecipientGroupSummaryProjection {
        Long getId();
        String getName();
        String getIcon();
        Integer getRecipientCount();
        Integer getStudentRecipientCount();
        Integer getParentRecipientCount();
        Instant getCreatedAt();
        Instant getUpdatedAt();
    }
}
