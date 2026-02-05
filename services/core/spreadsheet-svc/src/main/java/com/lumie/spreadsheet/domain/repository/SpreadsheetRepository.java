package com.lumie.spreadsheet.domain.repository;

import com.lumie.spreadsheet.domain.entity.Spreadsheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface SpreadsheetRepository {

    Spreadsheet save(Spreadsheet spreadsheet);

    Optional<Spreadsheet> findById(Long id);

    Page<Spreadsheet> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Spreadsheet> findAll(Pageable pageable);

    void deleteById(Long id);

    boolean existsById(Long id);
}
