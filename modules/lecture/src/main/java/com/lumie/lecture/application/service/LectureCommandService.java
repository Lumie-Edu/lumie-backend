package com.lumie.lecture.application.service;

import com.lumie.lecture.application.dto.request.CreateLectureRequest;
import com.lumie.lecture.application.dto.request.UpdateLectureRequest;
import com.lumie.lecture.application.dto.response.LectureResponse;
import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.exception.LectureNotFoundException;
import com.lumie.lecture.domain.repository.LectureRepository;
import com.lumie.lecture.domain.vo.LectureStatus;
import com.lumie.lecture.domain.vo.LectureVisibility;
import com.lumie.common.tenant.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LectureCommandService {

    private final LectureRepository lectureRepository;

    public LectureResponse create(CreateLectureRequest request) {
        Long teacherId = request.teacherId() != null
                ? request.teacherId()
                : UserContextHolder.getUserId();

        LectureVisibility visibility = request.visibility() != null
                ? LectureVisibility.valueOf(request.visibility())
                : LectureVisibility.TEACHER_ONLY;

        Lecture lecture = Lecture.create(
                request.classId(),
                teacherId,
                request.title(),
                request.description(),
                request.youtubeUrl(),
                visibility,
                request.lectureDate()
        );

        return LectureResponse.from(lectureRepository.save(lecture));
    }

    public LectureResponse update(Long id, UpdateLectureRequest request) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(LectureNotFoundException::new);

        LectureVisibility visibility = request.visibility() != null
                ? LectureVisibility.valueOf(request.visibility()) : null;
        LectureStatus status = request.status() != null
                ? LectureStatus.valueOf(request.status()) : null;

        lecture.update(request.title(), request.description(),
                visibility, status, request.lectureDate());

        return LectureResponse.from(lectureRepository.save(lecture));
    }

    public void delete(Long id) {
        Lecture lecture = lectureRepository.findById(id)
                .orElseThrow(LectureNotFoundException::new);
        lecture.close();
        lectureRepository.save(lecture);
    }
}
