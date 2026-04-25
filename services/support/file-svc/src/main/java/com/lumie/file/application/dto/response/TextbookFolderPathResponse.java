package com.lumie.file.application.dto.response;

import java.util.List;

public record TextbookFolderPathResponse(
        List<TextbookFolderResponse> ancestors
) {
    public static TextbookFolderPathResponse of(List<TextbookFolderResponse> ancestors) {
        return new TextbookFolderPathResponse(List.copyOf(ancestors));
    }
}
