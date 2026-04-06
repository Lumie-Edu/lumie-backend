package com.lumie.journal.adapter.out.persistence;

import com.lumie.journal.domain.entity.ClassJournal;
import com.lumie.journal.domain.repository.JournalRepository;
import com.lumie.journal.domain.vo.JournalSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JournalRepositoryAdapter implements JournalRepository {

    private final JpaJournalRepository jpaRepository;

    @Override
    public ClassJournal save(ClassJournal journal) {
        return jpaRepository.save(journal);
    }

    @Override
    public Optional<ClassJournal> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<ClassJournal> findAll(JournalSearchCriteria criteria, Pageable pageable) {
        Specification<ClassJournal> spec = toSpecification(criteria);
        return jpaRepository.findAll(spec, pageable);
    }

    @Override
    public Page<ClassJournal> findByClassId(Long classId, Pageable pageable) {
        return jpaRepository.findByClassId(classId, pageable);
    }

    @Override
    public Page<ClassJournal> findByTeacherId(Long teacherId, Pageable pageable) {
        return jpaRepository.findByTeacherId(teacherId, pageable);
    }

    @Override
    public boolean existsByClassIdAndLessonDate(Long classId, LocalDate lessonDate) {
        return jpaRepository.existsByClassIdAndLessonDate(classId, lessonDate);
    }

    private Specification<ClassJournal> toSpecification(JournalSearchCriteria criteria) {
        Specification<ClassJournal> spec = Specification.where(null);

        if (criteria.classId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("classId"), criteria.classId()));
        }
        if (criteria.academyId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("academyId"), criteria.academyId()));
        }
        if (criteria.teacherId() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("teacherId"), criteria.teacherId()));
        }
        if (criteria.status() != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("status"), criteria.status()));
        }
        if (criteria.startDate() != null) {
            spec = spec.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("lessonDate"), criteria.startDate()));
        }
        if (criteria.endDate() != null) {
            spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("lessonDate"), criteria.endDate()));
        }
        if (criteria.search() != null && !criteria.search().isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("title")), "%" + criteria.search().toLowerCase() + "%"));
        }

        return spec;
    }
}
