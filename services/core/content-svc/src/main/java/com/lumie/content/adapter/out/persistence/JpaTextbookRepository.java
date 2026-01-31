package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.Textbook;
import com.lumie.content.domain.vo.TextbookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaTextbookRepository extends JpaRepository<Textbook, Long> {

    Page<Textbook> findByAcademyId(Long academyId, Pageable pageable);

    Page<Textbook> findBySubject(String subject, Pageable pageable);

    List<Textbook> findByStatus(TextbookStatus status);
}
