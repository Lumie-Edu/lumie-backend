package com.lumie.spreadsheet.adapter.out.persistence;

import com.lumie.spreadsheet.domain.entity.Spreadsheet;
import com.lumie.spreadsheet.domain.repository.SpreadsheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SpreadsheetRepositoryAdapter implements SpreadsheetRepository {

    private final JpaSpreadsheetRepository jpaRepository;

    @Override
    public Spreadsheet save(Spreadsheet spreadsheet) {
        return jpaRepository.save(spreadsheet);
    }

    @Override
    public Optional<Spreadsheet> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<Spreadsheet> findByOwnerId(Long ownerId, Pageable pageable) {
        return jpaRepository.findByOwnerId(ownerId, pageable);
    }

    @Override
    public Page<Spreadsheet> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
