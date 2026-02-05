package com.lumie.spreadsheet.adapter.out.persistence;

import com.lumie.spreadsheet.domain.entity.Spreadsheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSpreadsheetRepository extends JpaRepository<Spreadsheet, Long> {

    Page<Spreadsheet> findByOwnerId(Long ownerId, Pageable pageable);
}
