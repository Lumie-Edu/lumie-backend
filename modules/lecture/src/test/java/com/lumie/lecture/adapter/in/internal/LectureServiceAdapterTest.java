package com.lumie.lecture.adapter.in.internal;

import com.lumie.lecture.application.port.out.LectureClassTargetPersistencePort;
import com.lumie.lecture.application.port.out.LectureClassTargetPersistencePort.LectureClassTargetRow;
import com.lumie.lecture.application.port.out.LecturePersistencePort;
import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.vo.LectureVisibility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LectureServiceAdapterTest {

    @Mock
    LecturePersistencePort lectures;

    @Mock
    LectureClassTargetPersistencePort targets;

    @Test
    void classListingBatchesAndGroupsTargetIds() {
        Lecture lecture = Lecture.create(7L, "Science", null, null,
                LectureVisibility.STUDENT, null);
        ReflectionTestUtils.setField(lecture, "id", 1L);
        ReflectionTestUtils.setField(lecture, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(lecture, "updatedAt", Instant.parse("2026-01-01T00:00:00Z"));
        given(lectures.findAll(any(), eq(PageRequest.of(0, 10,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))))
                .willReturn(new PageImpl<>(List.of(lecture), PageRequest.of(0, 10), 1));
        given(targets.findRowsByLectureIds(List.of(1L)))
                .willReturn(List.of(new LectureClassTargetRow(1L, 2L),
                        new LectureClassTargetRow(1L, 3L)));

        var result = new LectureServiceAdapter(lectures, targets)
                .getLecturesByClass("tenant-one", 2L, 0, 10);

        assertThat(result.lectures()).hasSize(1);
        assertThat(result.lectures().getFirst().classIds()).containsExactly(2L, 3L);
        assertThat(result.lectures().getFirst().classId()).isEqualTo(2L);
    }
}
