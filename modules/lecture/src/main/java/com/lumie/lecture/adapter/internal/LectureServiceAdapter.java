package com.lumie.lecture.adapter.internal;

import com.lumie.internal.lecture.LectureService;
import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.repository.LectureRepository;
import com.lumie.lecture.domain.vo.LectureSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureServiceAdapter implements LectureService {

    private final LectureRepository lectureRepository;

    @Override
    public Optional<LectureData> getLecture(String tenantSlug, Long lectureId) {
        return lectureRepository.findById(lectureId)
                .map(this::toData);
    }

    @Override
    public LectureListData getLecturesByClass(String tenantSlug, Long classId, int page, int size) {
        LectureSearchCriteria criteria = new LectureSearchCriteria(
                classId, null, null, null, null, null
        );

        Page<Lecture> lectures = lectureRepository.findAll(
                criteria, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return new LectureListData(
                lectures.getContent().stream().map(this::toData).toList(),
                lectures.getTotalElements(),
                page,
                size
        );
    }

    private LectureData toData(Lecture lecture) {
        return new LectureData(
                lecture.getId(),
                lecture.getClassId(),
                lecture.getTeacherId(),
                lecture.getTitle(),
                lecture.getDescription(),
                lecture.getVisibility().name(),
                lecture.getStatus().name(),
                lecture.getCreatedAt().toString(),
                lecture.getUpdatedAt().toString()
        );
    }
}
