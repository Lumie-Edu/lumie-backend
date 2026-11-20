package com.lumie.file.application.dto.request;

import com.lumie.file.domain.vo.EntityType;

public enum UploadableEntityType {
    ANNOUNCEMENT,
    QNA;

    public EntityType toEntityType() {
        return EntityType.valueOf(name());
    }
}
