package com.lumie.lecture.adapter.in.internal;

import com.lumie.lecture.api.LectureService;
import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.entity.LectureClassTarget;
import com.lumie.lecture.domain.repository.LectureClassTargetRepository;
import com.lumie.lecture.domain.repository.LectureRepository;
import com.lumie.lecture.domain.vo.LectureSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureServiceAdapter implements LectureService {

    private final LectureRepository lectureRepository;
    private final LectureClassTargetRepository targetRepository;

    @Override
    public Optional<LectureData> getLecture(String tenantSlug, Long lectureId) {
        return lectureRepository.findById(lectureId)
                .map(this::toData);
    }

    @Override
    public LectureListData getLecturesByClass(String tenantSlug, Long classId, int page, int size) {
        LectureSearchCriteria criteria = new LectureSearchCriteria(
                classId, null, null, null, null
        );

        Page<Lecture> lectures = lectureRepository.findAll(
                criteria, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<Long, List<Long>> classIdsByLecture = loadClassIds(lectures.getContent());

        return new LectureListData(
                lectures.getContent().stream()
                        .map(lecture -> toData(
                                lecture,
                                primaryFirst(
                                        classIdsByLecture.getOrDefault(lecture.getId(), List.of()),
                                        lecture.getClassId()
                                )
                        ))
                        .toList(),
                lectures.getTotalElements(),
                page,
                size
        );
    }

    private LectureData toData(Lecture lecture) {
        return toData(lecture, classIdsForData(lecture));
    }

    private LectureData toData(Lecture lecture, List<Long> classIds) {
        return new LectureData(
                lecture.getId(),
                classIds.isEmpty() ? lecture.getClassId() : classIds.get(0),
                classIds,
                lecture.getTeacherId(),
                lecture.getTitle(),
                lecture.getDescription(),
                lecture.getVisibility().name(),
                lecture.getCreatedAt().toString(),
                lecture.getUpdatedAt().toString()
        );
    }

    private List<Long> classIdsForData(Lecture lecture) {
        List<Long> classIds = targetRepository.findByLectureIdOrderByClassIdAsc(lecture.getId()).stream()
                .map(LectureClassTarget::getClassId)
                .toList();
        return primaryFirst(classIds, lecture.getClassId());
    }

    private Map<Long, List<Long>> loadClassIds(List<Lecture> lectures) {
        if (lectures.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Long>> grouped = new HashMap<>();
        targetRepository.findRowsByLectureIds(lectures.stream().map(Lecture::getId).toList())
                .forEach(row -> grouped.computeIfAbsent(row.getLectureId(), ignored -> new java.util.ArrayList<>())
                        .add(row.getClassId()));
        return grouped;
    }

    private static List<Long> primaryFirst(List<Long> classIds, Long primaryClassId) {
        if (classIds.isEmpty()) {
            return primaryClassId == null ? List.of() : List.of(primaryClassId);
        }
        if (primaryClassId == null || !classIds.contains(primaryClassId)) {
            return classIds;
        }
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(primaryClassId),
                        classIds.stream().filter(id -> !id.equals(primaryClassId))
                )
                .toList();
    }
}
