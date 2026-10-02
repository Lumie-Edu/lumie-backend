package com.lumie.lecture.domain.repository;

import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.entity.LectureClassTarget;
import com.lumie.lecture.domain.vo.LectureSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public interface LectureRepository extends JpaRepository<Lecture, Long>,
        JpaSpecificationExecutor<Lecture> {

    @Query("""
            SELECT DISTINCT l
            FROM Lecture l
            JOIN LectureClassTarget t ON t.lecture = l AND t.classId = :classId
            WHERE l.visibility = 'STUDENT'
            """)
    Page<Lecture> findVisibleByTargetClassId(@Param("classId") Long classId, Pageable pageable);

    /**
     * Search lectures by criteria. Defaults sort to createdAt DESC if the
     * caller has not specified one.
     */
    default Page<Lecture> findAll(LectureSearchCriteria criteria, Pageable pageable) {
        Specification<Lecture> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.classId() != null) {
                Subquery<Long> targetSubquery = query.subquery(Long.class);
                Root<LectureClassTarget> target = targetSubquery.from(LectureClassTarget.class);
                targetSubquery.select(target.get("id"))
                        .where(
                                cb.equal(target.get("lecture").get("id"), root.get("id")),
                                cb.equal(target.get("classId"), criteria.classId())
                        );
                predicates.add(cb.exists(targetSubquery));
                query.distinct(true);
            }
            if (criteria.teacherId() != null) {
                predicates.add(cb.equal(root.get("teacherId"), criteria.teacherId()));
            }
            if (criteria.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lectureDate"), criteria.startDate()));
            }
            if (criteria.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("lectureDate"), criteria.endDate()));
            }
            if (criteria.search() != null && !criteria.search().isBlank()) {
                String pattern = "%" + criteria.search().toLowerCase(Locale.ROOT) + "%";
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
