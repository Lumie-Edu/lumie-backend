package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.QnaBoard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public interface QnaBoardRepository extends JpaRepository<QnaBoard, Long>, JpaSpecificationExecutor<QnaBoard> {

    @EntityGraph(attributePaths = {"comments"})
    @Query("SELECT q FROM QnaBoard q WHERE q.id = :id")
    Optional<QnaBoard> findByIdWithComments(Long id);

    Page<QnaBoard> findByQnaUserId(Long qnaUserId, Pageable pageable);

    Page<QnaBoard> findByIsAnsweredFalse(Pageable pageable);

    Page<QnaBoard> findByIsAnswered(Boolean isAnswered, Pageable pageable);

    long countByIsAnsweredFalseAndCreatedAtBefore(Instant threshold);

    default Page<QnaBoard> search(String keyword, List<Long> userIds, Boolean isAnswered, Pageable pageable) {
        Specification<QnaBoard> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (isAnswered != null) {
                predicates.add(cb.equal(root.get("isAnswered"), isAnswered));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("qnaTitle")), pattern);
                Predicate contentMatch = cb.like(cb.lower(root.get("qnaContent")), pattern);

                if (userIds != null && !userIds.isEmpty()) {
                    Predicate userMatch = root.get("qnaUserId").in(userIds);
                    predicates.add(cb.or(titleMatch, contentMatch, userMatch));
                } else {
                    predicates.add(cb.or(titleMatch, contentMatch));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }

    default Page<QnaBoard> searchByUser(Long qnaUserId, String keyword, Boolean isAnswered, Pageable pageable) {
        Specification<QnaBoard> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("qnaUserId"), qnaUserId));
            if (isAnswered != null) {
                predicates.add(cb.equal(root.get("isAnswered"), isAnswered));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("qnaTitle")), pattern),
                        cb.like(cb.lower(root.get("qnaContent")), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
