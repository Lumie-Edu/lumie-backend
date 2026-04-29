package com.lumie.lecture.adapter.out.persistence;

import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.repository.LectureRepository;
import com.lumie.lecture.domain.vo.LectureSearchCriteria;
import com.lumie.lecture.domain.vo.LectureStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LectureRepositoryAdapter implements LectureRepository {

    private final JpaLectureRepository jpaRepository;
    private final EntityManager entityManager;

    @Override
    public Lecture save(Lecture lecture) {
        return jpaRepository.save(lecture);
    }

    @Override
    public Optional<Lecture> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<Lecture> findAll(LectureSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Lecture> countRoot = countQuery.from(Lecture.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<Lecture> dataQuery = cb.createQuery(Lecture.class);
        Root<Lecture> dataRoot = dataQuery.from(Lecture.class);
        List<Predicate> dataPredicates = buildPredicates(cb, dataRoot, criteria);
        dataQuery.where(dataPredicates.toArray(new Predicate[0]));

        // Sorting
        List<Order> orders = new ArrayList<>();
        for (Sort.Order sortOrder : pageable.getSort()) {
            if (sortOrder.isAscending()) {
                orders.add(cb.asc(dataRoot.get(sortOrder.getProperty())));
            } else {
                orders.add(cb.desc(dataRoot.get(sortOrder.getProperty())));
            }
        }
        if (orders.isEmpty()) {
            orders.add(cb.desc(dataRoot.get("createdAt")));
        }
        dataQuery.orderBy(orders);

        TypedQuery<Lecture> typedQuery = entityManager.createQuery(dataQuery);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(typedQuery.getResultList(), pageable, total);
    }

    @Override
    public Page<Lecture> findVisibleByClassId(Long classId, Pageable pageable) {
        return jpaRepository.findVisibleByClassId(classId, pageable);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Lecture> root,
                                             LectureSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        // Exclude CLOSED by default
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
        if (criteria.processingStatus() != null) {
            predicates.add(cb.equal(root.get("processingStatus"), criteria.processingStatus()));
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

        return predicates;
    }
}
