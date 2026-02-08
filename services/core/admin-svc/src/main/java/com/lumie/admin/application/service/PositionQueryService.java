package com.lumie.admin.application.service;

import com.lumie.admin.application.dto.PositionPermissionResponse;
import com.lumie.admin.application.dto.PositionResponse;
import com.lumie.admin.domain.exception.PositionNotFoundException;
import com.lumie.admin.domain.repository.PositionPermissionRepository;
import com.lumie.admin.domain.repository.PositionRepository;
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
    private final PositionPermissionRepository positionPermissionRepository;

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

    public List<PositionPermissionResponse> getPositionPermissions(Long positionId) {
        positionRepository.findById(positionId)
                .orElseThrow(() -> new PositionNotFoundException(positionId));
        return positionPermissionRepository.findByPositionId(positionId).stream()
                .map(PositionPermissionResponse::from)
                .toList();
    }
}
