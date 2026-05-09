package com.lumie.lecture.domain.repository;

import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.vo.LectureSearchCriteria;
import com.lumie.lecture.domain.vo.LectureStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.ArrayList;
import java.util.List;

public interface LectureRepository extends JpaRepository<Lecture, Long>,
        JpaSpecificationExecutor<Lecture> {

    @Query("SELECT l FROM Lecture l WHERE l.classId = :classId " +
           "AND l.status = 'ACTIVE' " +
           "AND l.visibility = 'STUDENT'")
    Page<Lecture> findVisibleByClassId(@Param("classId") Long classId, Pageable pageable);

    /**
     * Search lectures by criteria. Excludes CLOSED status by default; defaults
     * sort to createdAt DESC if the caller has not specified one.
     */
    default Page<Lecture> findAll(LectureSearchCriteria criteria, Pageable pageable) {
        Specification<Lecture> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), LectureStatus.CLOSED));
            if (criteria.classId() != null) {
                predicates.add(cb.equal(root.get("classId"), criteria.classId()));
            }
            if (criteria.teacherId() != null) {
                predicates.add(cb.equal(root.get("teacherId"), criteria.teacherId()));
            }
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (criteria.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lectureDate"), criteria.startDate()));
            }
            if (criteria.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("lectureDate"), criteria.endDate()));
            }
            if (criteria.search() != null && !criteria.search().isBlank()) {
                String pattern = "%" + criteria.search().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable effective = pageable.getSort().isUnsorted()
                ? org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(), pageable.getPageSize(),
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                : pageable;
        return findAll(spec, effective);
    }
}
