package com.lumie.academy.application.service;

import com.lumie.academy.application.dto.PositionResponse;
import com.lumie.academy.domain.exception.PositionNotFoundException;
import com.lumie.academy.domain.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PositionQueryService {

    private final PositionRepository positionRepository;

    public PositionResponse getPosition(Long id) {
        return positionRepository.findById(id)
                .map(PositionResponse::from)
                .orElseThrow(() -> new PositionNotFoundException(id));
    }

    public Page<PositionResponse> getAllPositions(Pageable pageable) {
        return positionRepository.findAll(pageable)
                .map(PositionResponse::from);
    }

    public List<PositionResponse> getActivePositions() {
        return positionRepository.findByIsActiveTrue().stream()
                .map(PositionResponse::from)
                .toList();
    }
}
