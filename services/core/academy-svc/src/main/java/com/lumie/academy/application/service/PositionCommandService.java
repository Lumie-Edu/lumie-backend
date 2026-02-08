package com.lumie.academy.application.service;

import com.lumie.academy.application.dto.PositionRequest;
import com.lumie.academy.application.dto.PositionResponse;
import com.lumie.academy.domain.entity.Position;
import com.lumie.academy.domain.exception.AcademyErrorCode;
import com.lumie.academy.domain.exception.DuplicatePositionNameException;
import com.lumie.academy.domain.exception.PositionNotFoundException;
import com.lumie.academy.domain.repository.AdminRepository;
import com.lumie.academy.domain.repository.PositionRepository;
import com.lumie.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PositionCommandService {

    private final PositionRepository positionRepository;
    private final AdminRepository adminRepository;

    public PositionResponse createPosition(PositionRequest request) {
        if (positionRepository.existsByName(request.name())) {
            throw new DuplicatePositionNameException(request.name());
        }

        Position position = Position.create(request.name());
        Position saved = positionRepository.save(position);

        log.info("Position created: {}", saved.getName());
        return PositionResponse.from(saved);
    }

    public PositionResponse updatePosition(Long id, PositionRequest request) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new PositionNotFoundException(id));

        if (request.name() != null && !request.name().equals(position.getName())
                && positionRepository.existsByName(request.name())) {
            throw new DuplicatePositionNameException(request.name());
        }

        position.updateName(request.name());
        Position updated = positionRepository.save(position);

        log.info("Position updated: {}", id);
        return PositionResponse.from(updated);
    }

    public void deactivatePosition(Long id) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new PositionNotFoundException(id));

        position.deactivate();
        positionRepository.save(position);

        log.info("Position deactivated: {}", id);
    }

    public void reactivatePosition(Long id) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new PositionNotFoundException(id));

        position.activate();
        positionRepository.save(position);

        log.info("Position reactivated: {}", id);
    }

    public void deletePosition(Long id) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new PositionNotFoundException(id));

        if (adminRepository.existsByPositionId(id)) {
            throw new BusinessException(AcademyErrorCode.POSITION_IN_USE,
                    "해당 직책을 사용 중인 직원이 있어 삭제할 수 없습니다.");
        }

        positionRepository.delete(position);

        log.info("Position deleted: {}", id);
    }
}
