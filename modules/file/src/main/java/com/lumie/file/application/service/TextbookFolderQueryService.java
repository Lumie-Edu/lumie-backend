package com.lumie.file.application.service;

import com.lumie.file.application.dto.response.TextbookFolderPathResponse;
import com.lumie.file.application.dto.response.TextbookFolderResponse;
import com.lumie.file.domain.entity.TextbookFolder;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.file.domain.repository.TextbookFolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TextbookFolderQueryService {

    private final TextbookFolderRepository textbookFolderRepository;

    @Transactional(readOnly = true)
    public List<TextbookFolderResponse> listFolders(UUID parentId) {
        return textbookFolderRepository.findByParentId(parentId).stream()
                .map(TextbookFolderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TextbookFolderResponse getFolder(UUID folderId) {
        TextbookFolder folder = textbookFolderRepository.findById(folderId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FOLDER_NOT_FOUND));
        return TextbookFolderResponse.from(folder);
    }

    @Transactional(readOnly = true)
    public TextbookFolderPathResponse getFolderPath(UUID folderId) {
        List<TextbookFolderResponse> ancestors = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();

        UUID currentId = folderId;
        while (currentId != null) {
            if (!visited.add(currentId)) {
                break; // cycle guard
            }
            TextbookFolder folder = textbookFolderRepository.findById(currentId)
                    .orElseThrow(() -> new BusinessException(FileErrorCode.FOLDER_NOT_FOUND));
            ancestors.add(TextbookFolderResponse.from(folder));
            currentId = folder.getParentId();
        }

        Collections.reverse(ancestors);
        return TextbookFolderPathResponse.of(ancestors);
    }
}
