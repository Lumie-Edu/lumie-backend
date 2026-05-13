package com.lumie.ai.adapter.in.web;

import com.lumie.ai.application.dto.ConversationDetailResponse;
import com.lumie.ai.application.dto.ConversationSummaryResponse;
import com.lumie.ai.application.service.ConversationQueryService;
import com.lumie.common.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationQueryService queryService;

    @GetMapping
    public ResponseEntity<PageResponse<ConversationSummaryResponse>> listConversations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(queryService.listConversations(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDetailResponse> getConversation(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getConversation(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        queryService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }
}
