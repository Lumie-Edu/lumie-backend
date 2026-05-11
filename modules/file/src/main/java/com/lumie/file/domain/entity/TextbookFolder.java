package com.lumie.file.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.file.domain.exception.FileErrorCode;
import com.lumie.file.domain.exception.FileException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "textbook_folders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TextbookFolder extends TenantScopedEntity {

    private static final int MAX_DEPTH = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "depth", nullable = false)
    private int depth;

    @Builder
    private TextbookFolder(String name, UUID parentId, int depth) {
        this.name = name;
        this.parentId = parentId;
        this.depth = depth;
    }

    public static TextbookFolder create(String name, UUID parentId, int parentDepth) {
        int newDepth = parentId == null ? 1 : parentDepth + 1;
        if (newDepth > MAX_DEPTH) {
            throw new FileException(FileErrorCode.MAX_DEPTH_EXCEEDED);
        }
        return TextbookFolder.builder()
                .name(name)
                .parentId(parentId)
                .depth(newDepth)
                .build();
    }

    public void rename(String newName) {
        this.name = newName;
    }
}
